/**
 * Copyright (c) {2003,2010} {openmobster@gmail.com} {individual contributors as indicated by the @authors tag}.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openmobster.core.cloud.console.client.ui.apn;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;  

import org.openmobster.core.cloud.console.client.rpc.DeviceService;
import org.openmobster.core.cloud.console.client.rpc.DeviceServiceAsync;
import org.openmobster.core.cloud.console.client.rpc.PushAppService;
import org.openmobster.core.cloud.console.client.rpc.PushAppServiceAsync;
import org.openmobster.core.cloud.console.client.state.ContextRegistry;
import org.openmobster.core.cloud.console.client.ui.Screen;
import org.openmobster.core.cloud.console.client.ui.TabController;
import org.openmobster.core.cloud.console.client.common.Constants;
import org.openmobster.core.cloud.console.client.common.Payload;
import org.openmobster.core.cloud.console.client.flow.FlowServiceRegistry;
import org.openmobster.core.cloud.console.client.flow.TransitionService;
import org.openmobster.core.cloud.console.client.model.Device;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.NamedFrame; 
import com.google.gwt.http.client.URL;

/**
 *
 * @author openmobster@gmail.com
 */
public class DownloadCertificateDialog implements Screen
{
	private StaticTextItem appIdText;
	private DynamicForm form;
	private PushAppRecord record;
	private UploadItem fileItem;
	private PasswordItem password;
	
	public DownloadCertificateDialog(PushAppRecord record)
	{
		this.record = record;
	}
	
	public String title()
	{
		return "downloadCertificateDialog";
	}
	
	public Canvas render()
	{
		this.initComplete(this);
		
		Window winModal = new Window();
		
		winModal.setWidth(360);
        winModal.setHeight(200);
        winModal.setTitle("Download Push Certificate");
        winModal.setShowMinimizeButton(false);
        winModal.setIsModal(true);
        winModal.setShowModalMask(true);
        winModal.centerInPage();
        winModal.setShowCloseButton(false);
        
        VLayout formLayout = new VLayout();
        
        //Create the Form
        form = new DynamicForm();
        form.setHeight100();
        form.setWidth100();
        form.setPadding(5);
        form.setLayoutAlign(VerticalAlignment.BOTTOM);
        form.setTarget(this.title());
        form.setEncoding(Encoding.MULTIPART);
        form.setAction(GWT.getModuleBaseURL() + "apn");
        
        this.appIdText = new StaticTextItem();
        this.appIdText.setTitle("App Id");
        this.appIdText.setValue(record.getAppId());
        
        this.fileItem = new UploadItem("certificate");
        this.fileItem.setTitle("Replace Cert");
        this.fileItem.setWidth(300);
        
        this.password = new PasswordItem("password");
        this.password.setTitle("Password");
        
        
        Button downloadButton = new Button("Download Cert");
        downloadButton.addClickHandler(new ClickHandler(){
        	public void onClick(ClickEvent e) 
        	{
        		String appId = URL.encode(record.getAppId());
        		com.google.gwt.user.client.Window.open(GWT.getModuleBaseURL() + "apn/certificate/download?appid="+appId,
        		"_blank",null);
        	}
        });
        
        Button replaceButton = new Button("Replace Cert");
        replaceButton.addClickHandler(new ClickHandler(){
        	public void onClick(ClickEvent e) 
        	{
        		Object obj = fileItem.getDisplayValue();
            	Object passwordVal = password.getValue();
            	if (obj != null && passwordVal != null) 
            	{
            		SC.showPrompt("Upload Certificate", "Upload in Progress");
            		form.submitForm();
            	} 
            	else
            	{
            		SC.say("All fields are required");
            	}
        	}
        });
        
        Button pushButton = new Button("Send Test Push");
        pushButton.addClickHandler(new ClickHandler(){
        	public void onClick(ClickEvent e) 
        	{
        		DownloadCertificateDialog.this.startTestPush();
        	}
        });
        

        Button stopPushButton = new Button("Stop Push");
        stopPushButton.addClickHandler(new ClickHandler(){
        	public void onClick(ClickEvent e) 
        	{
        		DownloadCertificateDialog.this.stopPush();
        	}
        });
        
        Button close = new Button();
        close.setTitle("Close");
        close.addClickHandler(new ClickHandler()
        {
            @Override
            public void onClick(ClickEvent event) 
            {
                FlowServiceRegistry.getTransitionService().closeActiveWindow();
            }
        });
          
        form.setFields(this.appIdText, this.fileItem, this.password);
        formLayout.addChild(form);
        
        //Button bar
        HLayout toolbar = new HLayout();
        toolbar.setAlign(Alignment.CENTER);
        toolbar.addMember(downloadButton);
        toolbar.addMember(replaceButton);
        toolbar.addMember(pushButton);
        
        
        //Second Button Bar
        HLayout toolbar2 = new HLayout();
        toolbar2.setAlign(Alignment.CENTER);
        toolbar2.addMember(stopPushButton);
        toolbar2.addMember(close);
        
        
        NamedFrame frame = new NamedFrame(this.title());
        frame.setWidth("1px");
        frame.setHeight("1px");
        frame.setVisible(false); 
        
        formLayout.addMember(frame);
        
        winModal.addItem(formLayout);
        winModal.addItem(toolbar);
        winModal.addItem(toolbar2);
        
		return winModal;
	}
	
	private void startTestPush()
	{
		SC.showPrompt("Loading....");
		
		final DeviceServiceAsync deviceService = GWT.create(DeviceService.class);
		String payload = Payload.encode(new String[]{"all"});
		
		deviceService.invoke(payload,new AsyncCallback<String>(){
			public void onFailure(Throwable caught) 
			{
				SC.clearPrompt();
				SC.say("System Error", "Unexpected Network Error. Please try again.",null);
			}
			
			public void onSuccess(String result)
			{
				SC.clearPrompt();
				if(result.trim().equals("500"))
				{
					//validation error
					SC.say("System Error", "Internal Server Error. Please try again.",null);
				}
				else
				{
					//A list of all accounts sent back to show some need activation
					List<Device> devices = Device.toList(result);
					ContextRegistry.getAppContext().setAttribute(Constants.devices, devices);
					
					TransitionService transitionService = FlowServiceRegistry.getTransitionService();
	        		transitionService.transitionActiveWindow(new TestPushDialog(record.getAppId()));
				}
			}
		});
	}
	
	private void stopPush()
	{
		SC.showPrompt("Stopping Push....");

		final PushAppServiceAsync service = GWT.create(PushAppService.class);
		String payload = Payload.encode(new String[]{"stopPush",DownloadCertificateDialog.this.record.getAppId()});

		service.invoke(payload,new AsyncCallback<String>(){
			public void onFailure(Throwable caught) 
			{
				SC.clearPrompt();
				SC.say("System Error", "Unexpected Network Error. Please try again.",null);
			}
			
			public void onSuccess(String result)
			{
				SC.clearPrompt();
				if(result.trim().equals("500"))
				{
					//validation error
					SC.say("System Error", "Internal Server Error. Please try again.",null);
				}
				else
				{
					//Just confirmation
					SC.say("Push Notifications for this App are successfully stopped", new BooleanCallback(){
						@Override
						public void execute(Boolean value) 
						{	
							FlowServiceRegistry.getTransitionService().closeActiveWindow();
					    	TabController.getInstance().closeTab(Constants.push_app);
					    	PushAppLoader.load(Constants.push_app);
						}
					});
				}
			}
		});
	}
	
	private native void initComplete(DownloadCertificateDialog upload) /*-{
	//$wnd.alert(upload);
	$wnd.uploadComplete = function (fileName) {
	upload.@org.openmobster.core.cloud.console.client.ui.apn.DownloadCertificateDialog::uploadComplete(Ljava/lang/String;)(fileName);
	};
	}-*/;
	
	public void uploadComplete(String fileName) 
	{
		SC.clearPrompt();
		SC.say("Certificate is stored successfully", new BooleanCallback(){
			@Override
			public void execute(Boolean value) 
			{	
				FlowServiceRegistry.getTransitionService().closeActiveWindow();
		    	TabController.getInstance().closeTab(Constants.push_app);
		    	PushAppLoader.load(Constants.push_app);
			}
		});
		
		this.fileItem.setValue("");
		this.password.setValue("");
	} 
}
