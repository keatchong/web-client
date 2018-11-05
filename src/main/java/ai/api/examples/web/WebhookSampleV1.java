package ai.api.examples.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.annotation.WebServlet;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import ai.api.model.AIOutputContext;
import ai.api.model.Fulfillment;
import ai.api.web.AIWebhookServletV1;

@WebServlet("/webhookV1")
public class WebhookSampleV1 extends AIWebhookServletV1 {
	
	private static final long serialVersionUID = 1L;
	private String  webServiceURL = "";
	
	@Override
	protected void doWebhook(AIWebhookRequest input, Fulfillment output, boolean stubMode, boolean localMode) {
		if ( stubMode ) {
			postToStub(input,output);
		} else {
			if (localMode) {
				webServiceURL = "http://localhost/th/webservice_json";
			} else {
				webServiceURL = "http://13.229.1.3/hrdemo/webservice_json";
			}
			postToWebservice(input,output);
		}
		
	}

	private String generateRequestJSON(AIWebhookRequest input) throws JsonProcessingException {
		
			ObjectMapper mapper = new ObjectMapper();
			String dateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
			mapper.setDateFormat(new SimpleDateFormat(dateFormat));
			HashMap<String, JsonElement> params = input.getResult().getParameters();
			AIOutputContext context = input.getResult().getContext("login-authentication");
			Map<String, JsonElement> contextParams  = context.getParameters();
			
			
			Map<String, Object> jsonMap = new HashMap<String, Object>();
			Map<String, Object> inputMap = new HashMap<String, Object>();
		
			jsonMap.put("action_name", input.getResult().getMetadata().getIntentName());
		
			String key = null;
			JsonElement value = null;
			
			Iterator<Entry<String, JsonElement>> it = contextParams.entrySet().iterator();
			while (it.hasNext()) {
		        Entry<String, JsonElement> pair = it.next();
		        key = pair.getKey();   		
		        
		       if ( ("access_key_id").equals(key) || "timestamp".equals(key) ||
		        		"signature".equals(key) || "tokenid".equals(key) ) {
		        	jsonMap.put((String) pair.getKey(),pair.getValue().getAsString());
		        } 
		   
			}
			
			it = params.entrySet().iterator();
			while (it.hasNext()) {
			        Entry<String, JsonElement> pair = it.next();
			        key = pair.getKey();   		
			        inputMap.put((String) pair.getKey(),pair.getValue().getAsString());
			}
			jsonMap.put("input",inputMap);
			
			/*{
				  "action_name": "leave.list_leave_entitlement",
				  "access_key_id": "8NvQ92ckO2yXP5qCDmrz",
				  "timestamp": "02/11/2018 00:00:00.0",
				  "signature": "UGrR9+J0p+2g63PQRF5Hru2/0eI=",
				  "tokenid" : "511c781a-85a7-4f40-80e5-49a1c64dc51b",
				  "input": {
				    "zyear": 2017
				  }
			}*/
			/*{
				  "action_name": "login",
				  "access_key_id":"8NvQ92ckO2yXP5qCDmrz",
				  "timestamp":"",
				  "signature":"dUw0ZJXqjBN75Wxd0HfdVITNflY=",
				  "input": {
				    "username": "ahmad.arif",
				    "password": "TstVnPgIHe1pvJDMK0V6Pw=="
				  
				  }
				}*/

			System.out.println("JSON Request to Web Service : " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap));
			return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonMap);
	}
	
	private void postToWebservice(AIWebhookRequest input,Fulfillment output) {
		
		HttpClient httpClient = HttpClientBuilder.create().build(); 
	    
		try {
			HttpPost request = new HttpPost(webServiceURL);
		    StringEntity params =new StringEntity(generateRequestJSON(input));
		    request.addHeader("content-type",  "application/json");
		    request.setEntity(params);
		    generateResponse( input,httpClient.execute(request),output);
		}catch (Exception ex) {
			//handle exception here
		} finally {
			httpClient.getConnectionManager().shutdown(); 
		}
		
	}
	
	private void postToStub(AIWebhookRequest input, Fulfillment output)  {

		JsonNode jsonInput = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			jsonInput = mapper.readTree(generateRequestJSON(input));
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		String response = "";
		if (jsonInput.path("action_name").asText().equals("login")) {
			response = "{\"results\": {\"stafftype\": 1,\"gender\": 1,\"tokenid\": \"511c781a-85a7-4f40-80e5-49a1c64dc51b\",\"staffname\": \"AHMAD ARIF BIN ABDUL AZIZ\"},\"status\": \"success\",\"success_msg\": \"Login successful\"}";
		} else if (jsonInput.path("action_name").asText().equals("leave.list_leave_entitlement")) {
			response = "{\"results\":{ \"RH02\":{ \"balance\":-6, \"leavetypedesc\":\"CUTIKECEMASAN\", \"leavetypecode\":\"RH02\", \"showbalance\":0 }, \"BK01\":{ \"balance\":-1, \"leavetypedesc\":\"CUTIGANTIAN-LEBIHMASA\", \"leavetypecode\":\"BK01\", \"showbalance\":1 }, \"TR02\":{ \"balance\":-3, \"leavetypedesc\":\"CUTITANPAREKOD-PEPERIKSAAN/BELAJAR\", \"leavetypecode\":\"TR02\", \"showbalance\":0 } }, \"status\":\"success\", \"success_msg\":\"\" }";
		}
		System.out.println("Response = " + response);
		generateStubResponse(input,response,output);
	}
	

	private void generateResponse(AIWebhookRequest input, HttpResponse response, Fulfillment output) throws IOException {
		
		JsonNode jsonResp = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			jsonResp = mapper.readTree(getBody(response));
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
	
		String jsonRespStr = "";
		try {
		        ObjectMapper mapper = new ObjectMapper();
		        Object json = mapper.readValue(jsonResp.toString(), Object.class);
		        jsonRespStr =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (Exception e) {
		       e.printStackTrace();
		}
		output.setDisplayText(jsonResp.toString());
		output.setSpeech(jsonResp.toString());
		output.setContextOut(prepareAuthenticationOutputContext(input,jsonResp));
	}

	private void generateStubResponse(AIWebhookRequest input,String jsonData, Fulfillment output)  {
		
		JsonNode jsonResp = null;
		try {
			ObjectMapper mapper = new ObjectMapper();
			jsonResp = mapper.readTree(jsonData);
		} catch (Exception ex) {
			ex.printStackTrace();
			return;
		}
		
		String jsonRespStr = "";
		try {
		        ObjectMapper mapper = new ObjectMapper();
		        Object json = mapper.readValue(jsonResp.toString(), Object.class);
		        jsonRespStr =  mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
		} catch (Exception e) {
		       e.printStackTrace();
		}
		output.setDisplayText(jsonResp.toString());
		output.setSpeech(jsonResp.toString());
		output.setContextOut(prepareAuthenticationOutputContext(input,jsonResp));
	}

	
	private AIOutputContext prepareAuthenticationOutputContext(AIWebhookRequest input,JsonNode jsonResp) {
		
		AIOutputContext  contextOut = input.getResult().getContext("login-authentication") == null?new AIOutputContext():input.getResult().getContext("login-authentication"); 
		if  ( contextOut.getName() == null || contextOut.getName().equals("")) {
			contextOut.setName("login-authentication");
		}
		Map<String, JsonElement> contextParams = contextOut.getParameters();
		
		if ( contextParams == null) {
			 contextOut.setParameters(new HashMap<String, JsonElement>());
		}
		
		String intentName = input.getResult().getMetadata().getIntentName();		
		if ( intentName  != null && !intentName.equals("") && intentName.equals("login")  ) {
			try {
				Gson gson = new Gson();
				Iterator<String> nodes = jsonResp.path("results").fieldNames();
				Map<String, JsonElement> tokenParam = contextOut.getParameters();
				while ( nodes.hasNext()) {
					String node = nodes.next();
					String value = jsonResp.path("results").path(node).asText();
					if ( value.contains(" ")) {
						value = '"' + value + '"';
					}
					JsonElement element = gson.fromJson(value, JsonElement.class);
					tokenParam.put(node, element);
				}
				contextOut.setParameters(tokenParam);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return contextOut;
		} else {
				return contextOut;
		}
		
	}
	
	/*private AIOutputContext prepareOutputContext(JsonNode jsonResp) {
		
		AIOutputContext contextOut = new AIOutputContext();
		contextOut.setName("DUMMY");
		contextOut.setLifespan(new Integer(500));
		
		try {
		Gson gson = new Gson();
		Iterator<String> nodes = jsonResp.path("results").fieldNames();
		Map<String, JsonElement> tokenParam = new HashMap();
		while ( nodes.hasNext()) {
			String node = nodes.next();
			String value = jsonResp.path("results").path(node).asText();
			if ( value.contains(" ")) {
				value = '"' + value + '"';
			}
			JsonElement element = gson.fromJson(value, JsonElement.class);
			tokenParam.put(node, element);
		}
		contextOut.setParameters(tokenParam);
		} catch (Exception e) {
			e.printStackTrace();
			
		}
		return contextOut;
	}*/
	
	private static String getBody(HttpResponse response) throws IOException {

	    String body = null;
	    StringBuilder stringBuilder = new StringBuilder();
	    BufferedReader bufferedReader = null;

	    InputStream inputStream = response.getEntity().getContent();
	    
	    try {
	    
	        if (inputStream != null) {
	            bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
	            char[] charBuffer = new char[128];
	            int bytesRead = -1;
	            while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
	                stringBuilder.append(charBuffer, 0, bytesRead);
	            }
	        } else {
	            stringBuilder.append("");
	        }
	    } catch (IOException ex) {
	        throw ex;
	    } finally {
	        if (bufferedReader != null) {
	            try {
	                bufferedReader.close();
	            } catch (IOException ex) {
	                throw ex;
	            }
	        }
	    }
	    body = stringBuilder.toString();
	    return body;
	}	

	//
	//	below are for backend offline testing
	//
	

}



