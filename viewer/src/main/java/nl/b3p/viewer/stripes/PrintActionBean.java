/*
 * Copyright (C) 2012 B3Partners B.V.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package nl.b3p.viewer.stripes;

import java.io.*;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;
import nl.b3p.viewer.print.Legend;
import nl.b3p.viewer.print.PrintInfo;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.log4j.Logger;
import org.apache.xmlgraphics.util.MimeConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author Roy Braam
 */
@UrlBinding("/action/print")
@StrictBinding
public class PrintActionBean implements ActionBean {
    private static final Log log = LogFactory.getLog(PrintActionBean.class);     
    protected static Logger fopLogger = Logger.getLogger("org.apache.fop");
    public static String xsl_A4_Landscape = "/WEB-INF/xsl/print/A4_Landscape.xsl";
    public static String xsl_A4_Portrait = "/WEB-INF/xsl/print/A4_Portrait.xsl";
    public static String xsl_A3_Landscape = "/WEB-INF/xsl/print/A3_Landscape.xsl";
    public static String xsl_A3_Portrait = "/WEB-INF/xsl/print/A3_Portrait.xsl";
    public static final String A4 = "a4";
    public static final String A3 = "a3";
    public static final String LANDSCAPE = "landscape";
    public static final String PORTRAIT = "portrait";
    public static SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", new Locale("NL"));
    @Validate
    private String params;
    
    private ActionBeanContext context;
    
    public Resolution print() throws JSONException, Exception {
        JSONObject jRequest = new JSONObject(params);
        
        //get the image url:
        String imageUrl= getImageUrl(params);
        //get the form settings
        final PrintInfo info = new PrintInfo();
        if (jRequest.has("title")){
            info.setTitle(jRequest.getString("title"));
        }
        if (jRequest.has("subtitle")){
            info.setSubtitle(jRequest.getString("subtitle"));
        }
        info.setDate(df.format(new Date()));        
        info.setImageUrl(imageUrl);
        if (jRequest.has("bbox")){
            info.setBbox(jRequest.getString("bbox"));
        }
        if (jRequest.has("extraTekst")){
            info.setRemark(jRequest.getString("extraTekst"));
        }
        if(jRequest.has("quality")){
            info.setQuality(jRequest.getInt("quality"));
        }
        /* !!!!temp skip the legend, WIP*/
        if (jRequest.has("includeLegend") && jRequest.getBoolean("includeLegend")){
            if(jRequest.has("legendUrl")){
                ArrayList<Legend> legends= new ArrayList<Legend>();
                JSONArray jarray=null;
                Object o = jRequest.get("legendUrl");
                if (o instanceof JSONArray){
                    jarray= (JSONArray)o;
                }else if (o instanceof String){
                    jarray = new JSONArray();
                    jarray.put(o);
                }
                for (int i=0; i < jarray.length();i++){
                    JSONObject legendJson = new JSONObject(jarray.getString(i));
                    Legend legend = new Legend(legendJson);   
                    legends.add(legend);
                }
                info.setLegendUrls(legends);
            }
        }
        if (jRequest.has("angle")){
            int angle = jRequest.getInt("angle");
            angle = angle % 360;
            //because the map viewport is rotated the actual map rotation is negative
            angle = 360-angle;
            info.setAngle(angle);
        }
        
        final String mimeType;
        if (jRequest.has("action") && jRequest.getString("action").equalsIgnoreCase("saveRTF")){
            mimeType=MimeConstants.MIME_RTF;
        }else{
            mimeType=MimeConstants.MIME_PDF;
        }
        
        //determine the correct template
        String pageFormat = jRequest.has("pageformat") ? jRequest.getString("pageformat") : A4;
        String orientation = jRequest.has("orientation") ? jRequest.getString("orientation") : PORTRAIT;
        final String templateUrl = getTemplateUrl(pageFormat,orientation);
        
        StreamingResolution res = new StreamingResolution(mimeType) {
            @Override
            public void stream(HttpServletResponse response) throws Exception {
                createOutput(info,mimeType, templateUrl,true,response);         
            }
        };
        return res;
        
    }
    public void createOutput(PrintInfo info, String mimeType, String template,
            boolean addJavascript, HttpServletResponse response) throws MalformedURLException, IOException {

        File xslFile = new File(template);
        String path = new File(xslFile.getParent()).toURI().toString();

        /* Setup fopfactory */
        FopFactory fopFactory = FopFactory.newInstance();

        /* Set BaseUrl so that fop knows paths to images etc... */
        fopFactory.setBaseURL(path);

        /* Setup output stream */
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            /* Construct fop */
            FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
            foUserAgent.setCreator("Gisviewer webapplicatie");
            foUserAgent.setProducer("B3Partners");

            Date now = new Date();
            foUserAgent.setCreationDate(now);
            foUserAgent.setTitle("Kaart");

            Fop fop = fopFactory.newFop(mimeType, foUserAgent, out);

            //String s=printInfoToString(info);
            /* Setup Jaxb */
            JAXBContext jc = JAXBContext.newInstance(PrintInfo.class);
            JAXBSource src = new JAXBSource(jc, info);
            
            JAXBContext jaxbContext = JAXBContext.newInstance(PrintInfo.class);
            StringWriter sw = new StringWriter();
            jaxbContext.createMarshaller().marshal(info, sw);
            String s = sw.toString();
            /* Setup xslt */
            Source xsltSrc = new StreamSource(xslFile);
            //xsltSrc.setSystemId(path);

            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(xsltSrc);

            Result res = new SAXResult(fop.getDefaultHandler());

            transformer.transform(src, res);

            /* Setup response */
            response.setContentType(mimeType);
            response.setContentLength(out.size());
			
            /* Set filename and extension */
            String filename = "Kaart_" + info.getDate();

            if (mimeType.equals(MimeConstants.MIME_PDF)) {
                filename += ".pdf";
            } else if (mimeType.equals(MimeConstants.MIME_RTF)) {
                filename += ".rtf";
            }

            response.setHeader("Content-Disposition", "attachment; filename=" + filename);

            //TODO: Postprocess pages to add javascript print
            /* use postprocessing with itext to add Javascript to output 
            if (addJavascript) {
                addJsToPdfOutput(out, response);
            } else {
                response.getOutputStream().write(out.toByteArray());
            }*/
            response.getOutputStream().write(out.toByteArray());
            
            response.getOutputStream().flush();

        } catch (Exception ex) {
            log.error("Fout tijdens print output: ", ex);
        } finally {
            out.close();
        }
    }      
    /**
     * Get the image url from the CombineImageAction
     * @param param the json as string with params needed to create the image
     * @return url to (combined)image.
     */
    private String getImageUrl(String param) throws Exception {   
        
        RedirectResolution cia = new RedirectResolution(CombineImageActionBean.class);
        RedirectResolution pa = new RedirectResolution(PrintActionBean.class);
        //cia.addParameter("params", param);
        String url= context.getRequest().getRequestURL().toString();
        url=url.replace(pa.getUrl(new Locale("NL")), cia.getUrl(new Locale("NL")));
        
        HttpClient client = new HttpClient();
        PostMethod method = null;
        try {
            method = new PostMethod(url);
            method.addParameter("params", param);
            int statusCode=client.executeMethod(method);            
            if (statusCode != HttpStatus.SC_OK) {
                throw new Exception("Error connecting to server. HTTP status code: " + statusCode);
            }
            JSONObject response = new JSONObject(method.getResponseBodyAsString());
            if (!response.getBoolean("success")){
                throw new Exception("Error getting image: "+response.getString("error"));
            }
            return response.getString("imageUrl");
            //return json;
        } finally {
            if (method!=null){
                method.releaseConnection();
            }
        }
    }
    
    private String getTemplateUrl(String pageFormat, String orientation) {
        if (A4.equalsIgnoreCase(pageFormat) && PORTRAIT.equalsIgnoreCase(orientation)){
            return context.getServletContext().getRealPath(xsl_A4_Portrait);
        }else if (A4.equalsIgnoreCase(pageFormat) && LANDSCAPE.equalsIgnoreCase(orientation)){
            return context.getServletContext().getRealPath(xsl_A4_Landscape);
        }else if (A3.equalsIgnoreCase(pageFormat) && PORTRAIT.equalsIgnoreCase(orientation)){
            return context.getServletContext().getRealPath(xsl_A3_Portrait);
        }else if (A3.equalsIgnoreCase(pageFormat) && LANDSCAPE.equalsIgnoreCase(orientation)){
            return context.getServletContext().getRealPath(xsl_A3_Landscape);
        }else{
            return context.getServletContext().getRealPath(xsl_A4_Portrait);
        }        
    }
    //<editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public ActionBeanContext getContext() {
        return context;
    }
    
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }
    
    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }
    //</editor-fold>

    private String printInfoToString(PrintInfo info) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(PrintInfo.class);
        Marshaller m = context.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter sw = new StringWriter();
        m.marshal(info, sw);
        String s=sw.toString();
        return s;
    }

    


    
}
