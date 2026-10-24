package com.example.saasvilcas.feature.profile.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.lifecycle.ViewModelProvider;
import com.example.saasvilcas.*;
import com.example.saasvilcas.feature.profile.domain.Announcement;
import com.example.saasvilcas.shared.ui.*;
import java.util.*;

public final class AnnouncementsModalFragment extends BrandedModalFragment {
    private ProfileViewModel model;
    private LinearLayout body;
    private String selected="",option="";
    private TextView error;
    private boolean markedOnOpen;
    @Override public void onCreate(Bundle state) {super.onCreate(state);if(state!=null) {selected=state.getString("selected","");option=state.getString("option","");}}
    @Override protected int contentLayout() {return R.layout.profile_component_body;}
    @Override protected int surfaceBackground() {return R.drawable.modal_bg_white;}
    @Override protected void bindContent(View content) {
        body=(LinearLayout)content; VilcasApplication app=(VilcasApplication)requireActivity().getApplication();
        model=new ViewModelProvider(requireParentFragment(),ProfileViewModel.factory(app.newProfileRepository(),app.auth,app.sessions)).get(ProfileViewModel.class);
        render(); model.reloadAnnouncements();
    }
    @Override public void onViewCreated(View view,Bundle state) {
        super.onViewCreated(view,state);
        model.announcements.observe(getViewLifecycleOwner(),values->{
            render();
            if(!markedOnOpen) {
                markedOnOpen=true; List<String> unseen=new ArrayList<>();
                for(Announcement item:values) if(!item.seen) unseen.add(item.id);
                model.markSeen(unseen);
            }
        });
        model.announcementsError.observe(getViewLifecycleOwner(),message->{if(error!=null&&!message.isEmpty()) error.setText(message);});
        model.operation.observe(getViewLifecycleOwner(),status->{
            if(!status.kind.equals("vote")&&!status.kind.equals("seen")) return;
            if(error!=null) {error.setText(status.busy?"Guardando voto…":status.error);error.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(),status.busy?R.color.vl_text_deep:R.color.vl_negative));}
            ProfileUi.busy(body,status.busy);
            if(status.success) {model.resetOperation();model.reloadAnnouncements();}
        });
    }
    private void render() {
        body.removeAllViews(); error=null;
        List<Announcement> values=model.announcements.getValue();
        Announcement active=null;
        if(values!=null) for(Announcement item:values) if(item.id.equals(selected)) active=item;
        if(active==null) {
            selected=""; ProfileUi.text(body,"Anuncios",true);
            if(values==null) ProfileUi.text(body,"Cargando anuncios…",false);
            else if(values.isEmpty()) ProfileUi.text(body,"No hay anuncios vigentes.",false);
            else for(Announcement item:values) {
                ProfileUi.button(body,item.title+(item.seen?"":" · Nuevo"),false,()->{
                    selected=item.id;option="";render(); if(!item.seen) model.markSeen(item.id);
                });
                ProfileUi.text(body,item.message,false);
            }
        } else {
            final Announcement announcement=active;
            ProfileUi.button(body,"← Todos los anuncios",false,()->{selected="";render();});
            ProfileUi.text(body,active.title,true);
            if(!active.image.isEmpty()) {
                ImageView image=new ImageView(requireContext()); body.addView(image,new LinearLayout.LayoutParams(-1,ProfileUi.dp(body,150)));
                image.setScaleType(ImageView.ScaleType.FIT_CENTER); image.setContentDescription(active.title);ProductImageLoader.load(image,active.image);
            }
            ProfileUi.text(body,active.expires.isEmpty()?"Sin vencimiento":"Vence "+active.expires.substring(0,Math.min(10,active.expires.length())),false);
            ProfileUi.text(body,active.message,false);
            if(!active.options.isEmpty()) {
                boolean voted=!active.votedOption.isEmpty();int total=0;
                for(Announcement.Option value:active.options) total+=value.votes==null?0:value.votes;
                double weighted=0; int index=0;
                for(Announcement.Option value:active.options) {
                    index++;weighted+=index*(value.votes==null?0:value.votes);
                    if(voted) {
                        int percent=total==0?0:Math.round((value.votes==null?0:value.votes)*100f/total);
                        ProfileUi.text(body,value.text+(value.id.equals(active.votedOption)?" · Tu voto":"")+" · "+percent+"%",false);
                    } else {
                        String label=(active.satisfaction?"★ ":"")+value.text;
                        ProfileUi.button(body,label,value.id.equals(option),()->{option=value.id;render();});
                    }
                }
                if(voted) {
                    ProfileUi.text(body,"¡Gracias por responder la encuesta!",true);
                    if(active.satisfaction) ProfileUi.text(body,String.format(java.util.Locale.getDefault(),"Promedio: %.1f / %d ★ (%d votos)",total==0?0:weighted/total,active.options.size(),total),false);
                } else ProfileUi.button(body,"Confirmar voto",true,()->{
                    if(option.isEmpty()) {error.setText("Selecciona una opción.");return;}
                    model.vote(announcement.id,option);
                }).setEnabled(!option.isEmpty());
            }
        }
        error=ProfileUi.error(body);
        String message=model.announcementsError.getValue(); if(message!=null&&!message.isEmpty()) error.setText(message);
        ProfileUi.button(body,"Actualizar anuncios",false,()->model.reloadAnnouncements());
        ProfileUi.button(body,"Cerrar",false,this::dismiss);
        ProfileViewModel.Operation operation=model.operation.getValue();
        ProfileUi.busy(body,operation!=null&&operation.kind.equals("vote")&&operation.busy);
        ViewEntranceAnimator.play(body);body.post(()->{if(isAdded()&&getView()!=null) refreshModalSize();});
    }
    @Override public void onSaveInstanceState(Bundle state) {super.onSaveInstanceState(state);state.putString("selected",selected);state.putString("option",option);}
    @Override public void onDestroyView() {body=null;error=null;super.onDestroyView();}
}
