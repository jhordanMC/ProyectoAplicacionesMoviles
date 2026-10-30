package com.example.saasvilcas.feature.sales.presentation;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.*;
import androidx.annotation.NonNull;
import java.util.*;
import java.util.concurrent.*;
import com.example.saasvilcas.feature.sales.domain.*;

/** Company-scoped cart and asynchronous POS state; no network work runs in a Fragment. */
public final class SalesViewModel extends ViewModel {
    public static final class State {
        public final boolean loading,expired;
        public final String message;
        State(boolean loading,String message,boolean expired) { this.loading=loading; this.message=message; this.expired=expired; }
    }
    public static final class Submission {
        public enum Stage { IDLE, SENDING, SUCCEEDED, FAILED, UNCERTAIN }
        public final Stage stage;
        public final SaleResult sale;
        public final String message;
        Submission(Stage stage,SaleResult sale,String message) { this.stage=stage; this.sale=sale; this.message=message; }
        public boolean busy() { return stage==Stage.SENDING; }
    }
    private final SalesRepository repository;
    private final ArrayList<SalesProduct> catalog=new ArrayList<>();
    private final Map<String,SalesProduct> known=new LinkedHashMap<>();
    private final ArrayList<SalesSite> locations=new ArrayList<>();
    private final LinkedHashSet<String> knownSizes=new LinkedHashSet<>();
    private final SalesCart cart=new SalesCart();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final MutableLiveData<State> state=new MutableLiveData<>(new State(false,"",false));
    private final MutableLiveData<Submission> submission=new MutableLiveData<>(new Submission(Submission.Stage.IDLE,null,""));
    private String search="",size="",site="",gender="";
    private int generation,nextOffset;
    private boolean initialized,hasMore,cleared,preview;
    private Runnable pending;
    public SalesViewModel(SalesRepository repository) {
        this.repository=repository;
        List<SalesProduct> initial=repository.getCatalog();
        if(!initial.isEmpty()) {
            preview=true; initialized=true; catalog.addAll(initial);
            for(SalesProduct product:initial) { known.put(product.id,product); knownSizes.addAll(product.sizes); }
            for(String name:initial.get(0).sites) locations.add(new SalesSite(name,name,"local"));
            if(initial.size()>1) { SalesProduct sample=initial.get(1); cart.put(null,sample,"41",sample.sites.get(0),1); }
        }
    }
    public LiveData<State> state() { return state; }
    public LiveData<Submission> submission() { return submission; }
    public boolean submitting() { return submission.getValue().busy(); }
    public void ensureLoaded() { if(!initialized) reload(); }
    public void reload() { schedule(false); }
    private void schedule(boolean debounce) {
        if(preview) { publish(false,"",false); return; }
        if(pending!=null) main.removeCallbacks(pending);
        int request=++generation;
        publish(true,"",false);
        SalesFilter filter=new SalesFilter(search,size,site,gender);
        pending=()->load(request,filter,0,true);
        main.postDelayed(pending,debounce?350:0);
    }
    public void loadMore() {
        if(!hasMore || state.getValue().loading) return;
        publish(true,"",false);
        load(generation,new SalesFilter(search,size,site,gender),nextOffset,false);
    }
    private void load(int request,SalesFilter filter,int offset,boolean replace) {
        worker.execute(()->{
            try {
                List<SalesSite> sites=initialized?null:repository.loadSites();
                SalesPage page=repository.loadProducts(filter,offset);
                main.post(()->{
                    if(cleared || request!=generation) return;
                    if(sites!=null) { locations.clear(); locations.addAll(sites); }
                    initialized=true;
                    if(replace) catalog.clear();
                    for(SalesProduct product:page.items) {
                        catalog.removeIf(item->item.id.equals(product.id)); catalog.add(product);
                        known.put(product.id,product);
                        for(String value:product.sizes) if(!"Sin talla".equals(value)) knownSizes.add(value);
                    }
                    nextOffset=page.nextOffset; hasMore=page.hasMore;
                    publish(false,"",false);
                });
            } catch(SalesException error) {
                main.post(()->{ if(!cleared && request==generation) publish(false,error.getMessage(),error.status==401); });
            } catch(RuntimeException error) {
                main.post(()->{ if(!cleared && request==generation) publish(false,"No se pudo cargar el catálogo.",false); });
            }
        });
    }
    public List<SalesProduct> products() {
        if(!preview) return Collections.unmodifiableList(catalog);
        ArrayList<SalesProduct> result=new ArrayList<>(); String term=search.trim().toLowerCase(Locale.ROOT);
        for(SalesProduct product:catalog) if(product.name.toLowerCase(Locale.ROOT).contains(term)
                && (size.isEmpty()||product.sizes.contains(size)) && (site.isEmpty()||product.sites.contains(site))
                && (gender.isEmpty()||product.gender.equals(gender))) result.add(product);
        return result;
    }
    public boolean hasMore() { return hasMore; }
    public List<String> sizes() { return new ArrayList<>(knownSizes); }
    public List<String> sites() { ArrayList<String> names=new ArrayList<>(); for(SalesSite value:locations) names.add(value.name); return names; }
    public List<SalesSite> locations() { return Collections.unmodifiableList(locations); }
    public List<SalesSite> registers() { ArrayList<SalesSite> registers=new ArrayList<>(); for(SalesSite location:locations) if(location.isRegister()) registers.add(location); return registers; }
    public String getSearch() { return search; }
    public String getSize() { return size; }
    public String getSite() { return site; }
    public String getGender() { return gender; }
    public void search(String value) { if(search.equals(value)) return; search=value; schedule(true); }
    public void size(String value) { if(size.equals(value)) return; size=value; schedule(false); }
    public void site(String value) { if(site.equals(value)) return; site=value; schedule(false); }
    public void gender(String value) { if(gender.equals(value)) return; gender=value; schedule(false); }
    public SalesProduct product(String id) {
        SalesProduct product=known.get(id);
        if(product==null) for(CartLine line:cart.getLines()) if(line.product.id.equals(id)) return line.product;
        if(product==null) throw new IllegalArgumentException("El producto ya no está disponible. Actualiza el catálogo.");
        return product;
    }
    public List<CartLine> lines() { return cart.getLines(); }
    public long total() { return cart.totalCents(); }
    public int units() { return cart.unitCount(); }
    public int reserved(SalesVariant variant,String previousKey) { return cart.reserved(variant.id,variant.siteId,previousKey); }
    public boolean assigned(String id) { for(SalesSite value:locations) if(value.id.equals(id)) return true; return false; }
    public void put(String key,SalesProduct product,String size,String site,int quantity) { put(key,product,size,site,quantity,0); }
    public void put(String key,SalesProduct product,String size,String site,int quantity,long discount) {
        if(submitting()) throw new IllegalArgumentException("Espera a que termine el registro de venta.");
        cart.put(key,product,size,site,quantity,discount); notifyCart();
    }
    public void put(String key,SalesProduct product,SalesVariant variant,int quantity,long discount) {
        if(submitting()) throw new IllegalArgumentException("Espera a que termine el registro de venta.");
        if(!assigned(variant.siteId)) throw new IllegalArgumentException("Esta sede no está asignada a tu cuenta.");
        cart.put(key,product,variant,quantity,discount); notifyCart();
    }
    public void remove(String key) { if(submitting()) return; cart.remove(key); notifyCart(); }
    private void notifyCart() { State current=state.getValue(); publish(current.loading,current.message,current.expired); }
    public void resetSubmission() { if(!submitting()) submission.setValue(new Submission(Submission.Stage.IDLE,null,"")); }
    public void submit(SaleDraft draft) {
        Submission old=submission.getValue();
        if(old.busy() || old.stage==Submission.Stage.SUCCEEDED || old.stage==Submission.Stage.UNCERTAIN) return;
        submission.setValue(new Submission(Submission.Stage.SENDING,null,"Subiendo comprobantes y registrando la venta…"));
        worker.execute(()->{
            try {
                SaleResult sale=repository.submit(draft);
                main.post(()->{
                    if(cleared) return;
                    cart.clear();
                    submission.setValue(new Submission(Submission.Stage.SUCCEEDED,sale,sale.pending
                            ?"Venta guardada sin conexión. Se enviará al recuperar internet.":"Venta registrada correctamente."));
                    publish(false,"",false); reload();
                });
            } catch(SalesException error) {
                main.post(()->{
                    if(cleared) return;
                    String message=error.uncertain?"No se pudo verificar si la venta se registró. Comprueba el historial web antes de repetirla.":error.getMessage();
                    submission.setValue(new Submission(error.uncertain?Submission.Stage.UNCERTAIN:Submission.Stage.FAILED,null,message));
                    if(error.status==401) publish(false,message,true);
                    else if(error.status==400||error.status==409) reload();
                });
            } catch(RuntimeException error) {
                main.post(()->{ if(!cleared) submission.setValue(new Submission(Submission.Stage.FAILED,null,"Revisa los datos de la venta antes de confirmar.")); });
            }
        });
    }
    private void publish(boolean busy,String message,boolean expired) { state.setValue(new State(busy,message,expired)); }
    @Override protected void onCleared() { cleared=true; generation++; if(pending!=null) main.removeCallbacks(pending); worker.shutdown(); }
    public static ViewModelProvider.Factory factory(SalesRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                if(!type.isAssignableFrom(SalesViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
                return type.cast(new SalesViewModel(repository));
            }
        };
    }
}
