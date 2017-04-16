package hello;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;;

// TODO check /echo http://localhost:8080/HelloWebSocket/websocket.html
//@ServerEndpoint("/websocket")
public class EchoServer {

	private static final List<EchoServer> connection = new ArrayList<>();
	private Session session;
	
	@OnOpen
	public void onOpen(Session session){
		this.session = session;
		connection.add(this);
		System.out.println(session.getId() + " has opened a connection");
		String message = session.getId() + " has opened a connection";
		broadcast(message);
	}
	
	@OnMessage
	public void onMessage(String message, Session session){
		System.out.println("Message from " + session.getId() + " : " + message);
//		try {
//			session.getBasicRemote().sendText("Message from " + session.getId() + " : " + message);
//		} catch (IOException ex){
//			ex.printStackTrace();
//		}
		String message1 = "Message from " + session.getId() + " : " + message;
		broadcast(message1);		
	}
	
	@OnClose
	public void onClose(Session session){
		System.out.println("Session " + session.getId() + " has ended.");
	}
	
	private static void broadcast(String message){
		for(EchoServer client : connection){
			try {
				client.session.getBasicRemote().sendText(message);
			} catch(IOException ex){
				ex.printStackTrace();
			}
		}
	}
	
}







