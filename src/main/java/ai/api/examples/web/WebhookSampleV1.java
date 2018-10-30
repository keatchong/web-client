package ai.api.examples.web;

import javax.servlet.annotation.WebServlet;

import ai.api.model.AIRequest;
import ai.api.model.Fulfillment;
import ai.api.web.AIWebhookServletV1;

@WebServlet("/webhookV1")
public class WebhookSampleV1 extends AIWebhookServletV1 {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doWebhook( ai.api.model.AIRequest input, Fulfillment output) {
	  output.setSpeech("OUTPUT : " + input.getSessionId());
	  // this is to call internal URL
	  // parameters are getting from input
  }


  
  
}
