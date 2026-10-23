package com.example.saasvilcas.feature.dashboard.presentation;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.example.saasvilcas.feature.dashboard.domain.*;
import java.util.*;
import java.util.concurrent.*;

/** Company-scoped filters; independent loads never block the whole screen or publish obsolete ranges. */
public final class DashboardViewModel extends ViewModel {
    public static final class PanelState {
        public final DashboardPanel data;
        public final boolean loading;
        public final String error;
        public final int status;
        PanelState(DashboardPanel data,boolean loading,String error,int status) {
            this.data=data; this.loading=loading; this.error=error; this.status=status;
        }
    }
    public static final class State {
        public final Map<DashboardSection,PanelState> panels;
        public final boolean expired;
        State(Map<DashboardSection,PanelState> panels,boolean expired) {
            this.panels=Collections.unmodifiableMap(new EnumMap<>(panels)); this.expired=expired;
        }
    }
    private final DashboardRepository repository;
    private final ExecutorService worker=Executors.newFixedThreadPool(4);
    private final Handler main=new Handler(Looper.getMainLooper());
    private final EnumMap<DashboardSection,PanelState> panels=new EnumMap<>(DashboardSection.class);
    private final EnumMap<DashboardSection,Future<?>> requests=new EnumMap<>(DashboardSection.class);
    private final EnumMap<DashboardSection,Integer> versions=new EnumMap<>(DashboardSection.class);
    private boolean expanded,cleared,expired;
    private DashboardFilter filter;
    public final MutableLiveData<State> state=new MutableLiveData<>();
    public DashboardViewModel(DashboardRepository repository) {
        this.repository=repository;
        long today=DashboardFilter.today(System.currentTimeMillis());
        filter=DashboardFilter.forPeriod(0,today,today,today);
        for(DashboardSection section:DashboardSection.values()) { panels.put(section,new PanelState(null,false,"",0)); versions.put(section,0); }
        publish();
    }
    public DashboardFilter filter() { return filter; }
    public void restoreFilter(DashboardFilter selected) { filter=selected; }
    public boolean isExpanded() { return expanded; }
    public void toggleExpanded() { expanded=!expanded; }
    public void select(DashboardFilter selected) {
        if(filter.period==selected.period && filter.fromDay==selected.fromDay && filter.toDay==selected.toDay
                && filter.chartToDay==selected.chartToDay) return;
        filter=selected;
        for(DashboardSection section:DashboardSection.values()) if(section.periodDependent) load(section);
    }
    public void reload() {
        if(filter.period!=4) {
            long today=DashboardFilter.today(System.currentTimeMillis());
            filter=DashboardFilter.forPeriod(filter.period,today,filter.fromDay,filter.toDay);
        }
        for(DashboardSection section:DashboardSection.values()) load(section);
    }
    public void retry(DashboardSection section) { if(!panels.get(section).loading) load(section); }
    private void load(DashboardSection section) {
        if(cleared||expired) return;
        Future<?> old=requests.remove(section); if(old!=null) old.cancel(true);
        int version=versions.get(section)+1; versions.put(section,version);
        DashboardFilter selected=filter;
        panels.put(section,new PanelState(null,true,"",0)); publish();
        requests.put(section,worker.submit(()->{
            try {
                DashboardPanel data=repository.load(section,selected);
                main.post(()-> { if(current(section,version)) {
                    panels.put(section,new PanelState(data,false,"",0)); requests.remove(section); publish();
                } });
            } catch(CancellationException ignored) {
            } catch(Exception error) {
                DashboardException api=error instanceof DashboardException?(DashboardException)error:null;
                int status=api==null?0:api.status;
                String message=api==null?"No se pudo cargar esta sección. Inténtalo nuevamente.":api.getMessage();
                main.post(()-> { if(current(section,version)) {
                    panels.put(section,new PanelState(null,false,message,status)); requests.remove(section);
                    if(status==401) { expired=true; for(Future<?> request:requests.values()) request.cancel(true); requests.clear(); }
                    publish();
                } });
            }
        }));
    }
    private boolean current(DashboardSection section,int version) { return !cleared&&!expired && versions.get(section)==version; }
    private void publish() { state.setValue(new State(panels,expired)); }
    @Override protected void onCleared() {
        cleared=true; for(Future<?> request:requests.values()) request.cancel(true);
        requests.clear(); worker.shutdownNow();
    }
    public static ViewModelProvider.Factory factory(DashboardRepository repository) {
        return new ViewModelProvider.Factory() {
            @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> type) {
                if(!type.isAssignableFrom(DashboardViewModel.class)) throw new IllegalArgumentException("Unknown ViewModel");
                return type.cast(new DashboardViewModel(repository));
            }
        };
    }
}
