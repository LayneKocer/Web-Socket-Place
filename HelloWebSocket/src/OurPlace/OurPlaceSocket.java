package OurPlace;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;


//TODO check /gridcanvas http://localhost:8080/HelloWebSocket/GridCanvasWebsocket.html
@ServerEndpoint("/GridCanvasWebsocket")
public class OurPlaceSocket {
	
	private static final List<OurPlaceSocket> connection = new ArrayList<>();
	private Session session;

	public static PlaceBoard board;

	public static long currentTime;
	public static long lastTime;
	public static final long waitTime = 5000;
	
	public OurPlaceSocket(){
		System.out.println("The Constructor was called");
	}
	
	@OnOpen
	public void onOpen(Session session){
		if(this.board == null){
			this.board = new PlaceBoard(75,75);
			this.board.loadBoard();
			lastTime = System.currentTimeMillis();
		}
		currentTime = System.currentTimeMillis();
		this.session = session;
		connection.add(this);

		String update1 = "{\"lastUpdate\":\"-1000000000-01-01T00:00:00Z\"}";
		String updateReq1 = "{\"type\":\"update\", \"msg\": "+update1+"  }";
		
		String message = board.handleRequest(updateReq1);
		// send {"updateType":"refresh", "updateContent":{"boardHeight":4, "boardWidth":4, "board": [[0,0,0,0],[0,0,3,0],[0,4,0,0],[5,0,0,0]] }, "updateTime":"2017-04-15T07:28:53.565Z"}
		if(currentTime - lastTime >= waitTime){
			System.out.println("Saving board");
			this.board.saveBoard(message);
			currentTime = System.currentTimeMillis();
			lastTime = System.currentTimeMillis();
		}
		try {
			session.getBasicRemote().sendText(message);
		} catch (IOException ex){
			ex.printStackTrace();
		}
		System.out.println(session.getId() + " has opened a connection");
		System.out.println(session.getId() + " sent request: " + updateReq1);
		System.out.println("Sending " + message + " to " + session.getId());
	}
	
	@OnMessage
	public void onMessage(String message, Session session){
//		System.out.println(session.getId() + " sent request: " + message);
		
		String message1 = board.handleRequest(message);
//		System.out.println("Message from " + session.getId() + " : " + message);
//		System.out.println("Sending " + message1 + " to " + session.getId());
		if(message1 == ""){
			// System.out.println("dont send empty!");			
		} else {
			try {
				session.getBasicRemote().sendText(message1);
			} catch (IOException ex){
				ex.printStackTrace();
			}
		}
	}
	
	@OnClose
	public void onClose(Session session){
		
		System.out.println("Session " + session.getId() + " has ended.");
		for(int i=0; i< connection.size(); i++){
			if(connection.get(i).session.getId() == session.getId() ){
				try {
					connection.get(i).session.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				connection.remove(i);
				System.out.println("Session " + session.getId() + " has been Removed.");
			}
		}
	}

//	@OnError
//	public void onError(Session session){
//		System.out.println("Session " + session.getId() + " has fucked");
//	}

	
}
