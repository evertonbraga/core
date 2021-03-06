/*
 * Copyright 2011-2012 Conventions Framework.  
 * 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 */
package org.conventionsframework.bean;

import org.conventionsframework.event.StatePullEvent;
import org.conventionsframework.event.StatePushEvent;
import org.conventionsframework.model.StateItem;

/*
 * Copyright 2011-2012 Conventions Framework.  *  * Licensed under the Apache License, Version 2.0 (the "License");  * you may not use this file except in compliance with the License.  * You may obtain a copy of the License at  *  *      http://www.apache.org/licenses/LICENSE-2.0  *  * Unless required by applicable law or agreed to in writing, software  * distributed under the License is distributed on an "AS IS" BASIS,  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  * See the License for the specific language governing permissions and  * limitations under the License.
 * 
 */

import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.faces.application.Application;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.faces.event.MethodExpressionActionListener;
import javax.inject.Inject;
import javax.inject.Named;
import org.conventionsframework.producer.ResourceBundleProvider;
import org.primefaces.component.menuitem.MenuItem;
import org.primefaces.model.DefaultMenuModel;
import org.primefaces.model.MenuModel;

/**
 *
 * @author rmpestano Aug 8, 2011 7:44:09 PM
 */

@Named
@SessionScoped
public class StateController implements Serializable{
    private int STACK_SIZE = 5;
    
    private LinkedList<StateItem> stateItens = new LinkedList<StateItem>();
    @Inject 
    private Event<StatePullEvent> statePullEvent;
    
    @Inject
    private ResourceBundleProvider resourceBundleProvider;
    
    private MenuModel stateModel;

    public StateController() {
        stateItens = new LinkedList<StateItem>();
        String stackSize = FacesContext.getCurrentInstance().getExternalContext().getInitParameter("STACK_SIZE");
        if(stackSize != null){
            try{
                STACK_SIZE = Integer.parseInt(stackSize);
            }catch(Exception e){
                STACK_SIZE = 5;
            }
        }
        buildMenuModel();
    }
    
    public void onStatePush(@Observes StatePushEvent stackPushEvent){
        if(stateItens.contains(stackPushEvent.getStackItem())){
            stateItens.remove(stackPushEvent.getStackItem());
        }
        if(stateItens.size() == STACK_SIZE){
            stateItens.removeFirst();
        }
        stateItens.add(stackPushEvent.getStackItem());
        buildMenuModel();
    }
    
    /**
     * Remove all itens in 'front' of the clicked item 
     * @param item
     */
    public void pullStateItem(StateItem item){
        int itemIndex = stateItens.indexOf(item);
        statePullEvent.fire(new StatePullEvent(item));
        if(itemIndex != -1){
            Iterator<StateItem> i = stateItens.iterator();
            while(i.hasNext()){
                StateItem stateItem = i.next();
                if(stateItens.indexOf(stateItem) > itemIndex){
                    i.remove();
                }
            }
        }
        buildMenuModel();
    }
    
    public void clearState(){
        stateItens.clear();
    }
    
     public String clearStateAndGoHome(){
        stateItens.clear();
        return "home";
    }
    
     public int getStateItensSize(){
        return stateItens.size();
    }
    
     /**
      * action called by the breadCrumb commandLink
      * @param item
      * @return page to redirect
      */
    public String callAction(StateItem item){
        pullStateItem(item);
        return item.getPage();
    }   
    
    /**
     * action called by backButton
     * @return page to redirect
     */
    public String goBack(){
        stateItens.removeLast();
        if(stateItens.isEmpty()){
        try {
             //if stateItens is empty go to index
              FacesContext.getCurrentInstance().getExternalContext().redirect(FacesContext.getCurrentInstance().getExternalContext().getRequestContextPath());
              return null;
            } catch (IOException ex) {
                Logger.getLogger(StateController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
         
         //update data in the managed bean
        StateItem item = stateItens.peekLast();
        statePullEvent.fire(new StatePullEvent(item));
        buildMenuModel();
        if(!"".equals(item.getCallback())){
            Application application = FacesContext.getCurrentInstance().getApplication(); 
            ExpressionFactory expressionFactory = application.getExpressionFactory( ); 
            ELContext el = FacesContext.getCurrentInstance().getELContext();
            try{
                MethodExpression me = expressionFactory.createMethodExpression(el,item.getCallback(),Void.class, null);
                me.invoke(el, null);
            }catch(NullPointerException ex){
                return null;
            }
            return null;
        }    
        return item.getPage();
    }  
    

    private void buildMenuModel(){
           stateModel = new DefaultMenuModel();  
           MenuItem homeItem = new MenuItem();  
           homeItem.setAjax(false);
           homeItem.setValue("home");
           homeItem.setId("home_"+UUID.randomUUID().toString().replaceAll("-", ""));
           homeItem.setImmediate(true);
           homeItem.setActionExpression(this.createMethodExpression("#{stateController.clearStateAndGoHome}", String.class,new Class[]{}));
           stateModel.addMenuItem(homeItem);
         for (StateItem stateItem : stateItens) {
              MenuItem item = new MenuItem();  
              item.setAjax(stateItem.isAjax());
              item.setUpdate(stateItem.getUpdate());
              item.setValue(this.getItemTitle(stateItem.getTitle())); 
              item.setId("item_"+UUID.randomUUID().toString().replaceAll("-", ""));
              if(!"".equals(stateItem.getPage())){
                 item.setActionExpression(this.createMethodExpression(stateItem.getPage(), String.class,new Class[]{}));
              }
              if(!"".equals(stateItem.getCallback())){
                  item.setActionExpression(this.createMethodExpression(stateItem.getCallback(), String.class,new Class[]{}));
              }   
              Class[] args = new Class[]{ActionEvent.class};
              item.addActionListener(new MethodExpressionActionListener(this.createMethodExpression("#{stateController.callActionListener}",Void.TYPE,args)));
              item.getAttributes().put("stateItem", stateItem);
              item.setImmediate(true);
              if(stateItens.indexOf(stateItem) == stateItens.size()-1){
                  item.setDisabled(true);
                  item.setStyleClass("ui-state-disabled");
              }
              stateModel.addMenuItem(item);
        }
         
    }
    
   public void callActionListener(ActionEvent event){
      StateItem item = (StateItem) event.getComponent().getAttributes().get("stateItem");
      this.pullStateItem(item);
   }

    public MenuModel getStateModel() {
        return stateModel;
    }

    public void setStateModel(MenuModel stateModel) {
        this.stateModel = stateModel;
    }
    
      /**
       * 
       * @return Action do JSF criada na mão
       */
    public MethodExpression createMethodExpression(String action, Class<?> valueType, Class<?>[] expectedParamTypes){
         MethodExpression methodExpression = null;
         try{
             ExpressionFactory factory = FacesContext.getCurrentInstance().getApplication().getExpressionFactory();
             methodExpression = factory.createMethodExpression(FacesContext.getCurrentInstance().getELContext(), action, valueType, expectedParamTypes);
         }catch(Exception ex){
             ex.printStackTrace();
         }
         return methodExpression;
    } 
    
     public MethodExpressionActionListener getActionListenerExp(String actionListenerName){
         FacesContext context = FacesContext.getCurrentInstance();
         MethodExpression mExp = context.getApplication().getExpressionFactory()
                 .createMethodExpression(context.getELContext(), actionListenerName, null,
                 new Class[] {ActionEvent.class});
         return new MethodExpressionActionListener(mExp);
     }
         

    public LinkedList<StateItem> getStateItens() {
        return stateItens;
    }
   
    public void setStateItens(LinkedList<StateItem> stateItems) {
        this.stateItens = stateItems;
    }

    private String getItemTitle(String title) {
        if(resourceBundleProvider.getCurrentBundle() == null){
            return title;
        }
         String i18nTitle = null;
         try{
             i18nTitle  = resourceBundleProvider.getCurrentBundle().getString(title);
         }catch(java.util.MissingResourceException re){
             
         }
         if(i18nTitle != null){
                  return i18nTitle;
          } 
         return title;
    }
    
}
