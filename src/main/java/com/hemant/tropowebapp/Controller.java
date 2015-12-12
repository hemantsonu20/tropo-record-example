package com.hemant.tropowebapp;

import static com.voxeo.tropo.Key.ATTEMPTS;
import static com.voxeo.tropo.Key.BARGEIN;
import static com.voxeo.tropo.Key.EMAIL_FORMAT;
import static com.voxeo.tropo.Key.ID;
import static com.voxeo.tropo.Key.INTERDIGIT_TIMEOUT;
import static com.voxeo.tropo.Key.MAX_SILENCE;
import static com.voxeo.tropo.Key.NAME;
import static com.voxeo.tropo.Key.TERMINATOR;
import static com.voxeo.tropo.Key.TIMEOUT;
import static com.voxeo.tropo.Key.URL;
import static com.voxeo.tropo.Key.VALUE;
import static com.voxeo.tropo.Key.createKey;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.voxeo.tropo.Tropo;
import com.voxeo.tropo.TropoResult;
import com.voxeo.tropo.actions.Do;
import com.voxeo.tropo.actions.RecordAction;

@Path("/")
public class Controller {
    
    @Context
    private UriInfo info;
    
    private static final int DEFAULT_BUFFER_SIZE = 10240;
    
    @POST
    @Path("record")
    public Response record(TropoSessionBean sessionBean) {
    
        System.out.println("*****session*****");
        System.out.println(sessionBean.getSession());
        
        return Response.status(200).entity(formRecordSampleBody()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        
    }
    
    @POST
    @Path("continue")
    public Response continueEvent(String json) throws IOException {
    
        Tropo tropo = new Tropo();
        TropoResult sessionResult = tropo.parse(json);
        
        System.out.println("******result***********");
        System.out.println(sessionResult);
        
        return Response.ok().entity(formHangupResponse()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        
    }
    
    @POST
    @Path("fail")
    public Response error(String json) {
    
        return Response.ok().entity(formHangupResponse()).header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).build();
        
    }
    
    @POST
    @Path("dump")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response dump(@FormDataParam("filename") InputStream in, @FormDataParam("filename") FormDataContentDisposition fileDetail)
            throws IOException {
    
        try {
            processBody(in, fileDetail);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return Response.ok().build();
    }
    
    private void processBody(InputStream in, FormDataContentDisposition fileDetail) throws IOException {
    
        String fileName = null;
        if (fileDetail != null) {
            fileName = fileDetail.getFileName();
        }
        else {
            fileName = "recording.wav";
        }
        
        String prefix = fileName;
        String suffix = "";
        if (fileName.contains(".")) {
            prefix = fileName.substring(0, fileName.lastIndexOf('.'));
            suffix = fileName.substring(fileName.lastIndexOf('.'));
        }
        
        File attachment = File.createTempFile("recording-" + prefix, suffix);
        
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(attachment), DEFAULT_BUFFER_SIZE)) {
            
            byte buffer[] = new byte[DEFAULT_BUFFER_SIZE];
            
            for (int length = 0; (length = in.read(buffer)) > 0;) {
                out.write(buffer, 0, length);
            }
            
        }
        System.out.println("your recording saved at " + attachment.getAbsolutePath());
        
    }
    
    private String formRecordSampleBody() {
    
        Tropo tropo = new Tropo();
        
        tropo.on("continue", "/continue").say("your message has been recorded");
        
        tropo.on("incomplete", "/fail").say("no message recorded, good bye");
        
        tropo.on("hangup", "/fail");
        
        tropo.on("error", "/fail");
        
        RecordAction recordAction = tropo.record(NAME("phprecord"), URL(info.getBaseUri() + "dump"), BARGEIN(false), MAX_SILENCE(5.0f),
                createKey("maxTime", 300.0f), ATTEMPTS(2), TIMEOUT(10.0f), INTERDIGIT_TIMEOUT(1));
        
        recordAction.and(Do.say(VALUE("Please record your message after the beep, press pound to finish the recording")));
        
        recordAction.transcription(ID("phprecord-transcription"), URL("mailto:hemantsonu20@gmail.com"), EMAIL_FORMAT("encoded"));
        recordAction.choices(TERMINATOR("#"));
        
        return tropo.text();
    }
    
    private String formHangupResponse() {
    
        Tropo tropo = new Tropo();
        tropo.hangup();
        return tropo.text();
        
    }
    
}