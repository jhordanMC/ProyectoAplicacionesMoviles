package com.example.saasvilcas.feature.inventory.presentation;

import androidx.lifecycle.*;
import androidx.annotation.NonNull;
import android.os.Handler;
import android.os.Looper;
import java.util.*;
import java.util.concurrent.*;
import com.example.saasvilcas.feature.inventory.domain.*;

/** Company-scoped state. All repository I/O is serialized on a worker. */
public final class InventoryViewModel extends ViewModel {
    public static final class State {
        public final boolean loading,busy,expired,uncertain;
        public final String key,message;
        public final boolean success;
        State(boolean loading,boolean busy,String key,String message,boolean success,boolean expired,boolean uncertain) {
            this.loading=loading; this.busy=busy; this.key=key; this.message=message; this.success=success; this.expired=expired; this.uncertain=uncertain;
        }
    }
    private final InventoryRepository repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public final MutableLiveData<Integer> revision=new MutableLiveData<>(0);
    public final MutableLiveData<State> state=new MutableLiveData<>(new State(false,false,"","",false,false,false));
    public String search="",category="",gender="";
    private boolean initialized,cleared;
    private int generation;
    private Runnable pending;
    private final Map<String,State> outcomes=new LinkedHashMap<>();
    public State outcome(String key) { return outcomes.get(key); }
    public InventoryViewModel(InventoryRepository repository) { this.repository=repository; }
    public List<InventoryProduct> products() {
        List<InventoryProduct> result=new ArrayList<>();
        for(InventoryProduct p:repository.products()) if(p.name.toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT).trim())
            && (category.isEmpty()||category.equals(p.category)) && (gender.isEmpty()||gender.equals(p.gender))) result.add(p);
        return result;
    }
    public InventoryProduct product(String id) {
        for(InventoryProduct p:repository.products()) if(p.id.equals(id)) return p;
        throw new IllegalArgumentException("El producto ya no existe.");
    }
    public List<String> references(int kind) { return repository.references(kind); }
    public String[] warehouseDetails(String name) { return repository.warehouseDetails(name); }
    public boolean hasMore() { return repository.hasMore(); }
    public void ensureLoaded() { if(!initialized) reload(); }
    public void filtersChanged(boolean debounce) {
        if(pending!=null) main.removeCallbacks(pending);
        int request=++generation;
        pending=()-> { if(request==generation) reload(false); };
        main.postDelayed(pending,debounce?350:0);
    }
    public void reload() {
        reload(true);
    }
    private void reload(boolean references) {
        if(state.getValue().busy) return;
        int request=++generation; String term=search,cat=category,sex=gender;
        state.setValue(new State(true,false,"","",false,false,false));
        worker.execute(()->{
            try { if(references) repository.invalidateReferences(); repository.filter(term,cat,sex); repository.refresh(); main.post(()->{
                if(cleared||request!=generation) return; initialized=true; changed(); state.setValue(new State(false,false,"","",false,false,false));
            }); } catch(Exception error) { main.post(()-> { if(!cleared&&request==generation) fail("",error,false); }); }
        });
    }
    public void loadMore() {
        if(state.getValue().busy||state.getValue().loading||!hasMore()) return;
        int request=generation; state.setValue(new State(true,false,"","",false,false,false));
        worker.execute(()->{
            try { repository.loadMore(); main.post(()-> { if(!cleared&&request==generation) { changed(); state.setValue(new State(false,false,"","",false,false,false)); } }); }
            catch(Exception error) { main.post(()-> { if(!cleared&&request==generation) fail("",error,false); }); }
        });
    }
    public void detail(String id,String key) { perform(key,()->repository.detail(id),false); }
    public void save(InventoryProduct product,String key) { perform(key,()->repository.save(product),true); }
    public void duplicate(String id) { perform("duplicate",()->{
        InventoryProduct source=repository.detail(id); List<InventoryVariant> copy=new ArrayList<>();
        for(InventoryVariant v:source.variants) copy.add(new InventoryVariant(v.size,v.type,v.location,v.stock,"","",v.locationId,null));
        repository.save(new InventoryProduct(UUID.randomUUID().toString(),source.name+" (copia)",source.category,source.gender,source.supplier,source.price,source.cost,source.photo,copy));
    },true); }
    public void delete(String id,String key) { perform(key,()->repository.delete(id),true); }
    public void saveReference(int kind,String previous,String name,String key) {
        perform(key,()->repository.saveReference(kind,previous,name),true);
        if(kind==0&&Objects.equals(category,previous)) category=name.trim();
    }
    public void deleteReference(int kind,String name,String key) { perform(key,()->repository.deleteReference(kind,name),true); if(kind==0&&category.equals(name)) category=""; }
    public void saveWarehouse(String previous,String name,String address,String description,String key) {
        perform(key,()->repository.saveWarehouse(previous,name,address,description),true);
    }
    private void perform(String key,Runnable operation,boolean mutation) {
        if(state.getValue().busy||state.getValue().loading) return;
        if(state.getValue().uncertain && state.getValue().key.equals(key)) return;
        outcomes.remove(key);
        state.setValue(new State(false,true,key,mutation?"Guardando cambios…":"Cargando producto…",false,false,false));
        worker.execute(()-> {
            try {
                operation.run();
                main.post(()-> { if(!cleared) {
                    State result=new State(false,false,key,mutation?"Cambios guardados.":"",true,false,false);
                    remember(result); changed(); state.setValue(result); if(mutation) reload(false);
                } });
            } catch(Exception error) { main.post(()-> { if(!cleared) fail(key,error,mutation); }); }
        });
    }
    private void fail(String key,Exception error,boolean mutation) {
        InventoryException api=error instanceof InventoryException?(InventoryException)error:null;
        boolean uncertain=api!=null&&api.uncertain;
        String message=uncertain?"No se pudo verificar el cambio. Actualiza inventario y comprueba el resultado antes de repetir.":
            error instanceof IllegalArgumentException?error.getMessage():"No se pudo completar la operación de inventario.";
        State result=new State(false,false,key,message,false,api!=null&&api.status==401,uncertain);
        if(!key.isEmpty()) remember(result);
        state.setValue(result);
    }
    private void remember(State result) { outcomes.put(result.key,result); if(outcomes.size()>32) outcomes.remove(outcomes.keySet().iterator().next()); }
    private void changed() { revision.setValue(revision.getValue()+1); }
    @Override protected void onCleared() { cleared=true; generation++; if(pending!=null) main.removeCallbacks(pending); worker.shutdown(); }
    public static InventoryViewModel obtain(androidx.fragment.app.Fragment company) {
        return new ViewModelProvider(company,new ViewModelProvider.Factory() {
            @NonNull public <T extends ViewModel> T create(@NonNull Class<T> type) {
                return type.cast(new InventoryViewModel(((com.example.saasvilcas.VilcasApplication)company.requireActivity().getApplication()).newInventoryRepository()));
            }
        }).get(InventoryViewModel.class);
    }
}
