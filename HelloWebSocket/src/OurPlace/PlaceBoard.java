package OurPlace;

import java.time.*;
import java.util.Vector;
//import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class PlaceBoard {
	
	private int height;
	private int width;
	private int[][] data;
	private Vector<PixelChange> changes;
	private int trimSize;
	private Instant oldest;

/*	public static void main(String[] args) throws InterruptedException {
		PlaceBoard board = new PlaceBoard(4,4);

		String place1 = "{\"x\":2, \"y\":1, \"color\":3}";
		String place2 = "{\"x\":1, \"y\":2, \"color\":4}";
		String place3 = "{\"x\":0, \"y\":3, \"color\":5}";
		String update1 = "{\"lastUpdate\":\"-1000000000-01-01T00:00:00Z\"}";
		
		String updateReq1 = "{\"type\":\"update\", \"msg\": "+update1+"  }";
		String placeReq1 = "{\"type\":\"place\", \"msg\": "+place1+" }";
		String placeReq2 = "{\"type\":\"place\", \"msg\": "+place2+" }";
		String placeReq3 = "{\"type\":\"place\", \"msg\": "+place3+" }";
		
		board.printBoard();
		TimeUnit.MILLISECONDS.sleep(100);
		String t1 = Instant.now().toString();
		System.out.println( board.handleRequest(updateReq1) );
		System.out.println( board.handleRequest(placeReq1) );
		TimeUnit.MILLISECONDS.sleep(100);
		System.out.println( board.handleRequest(placeReq2) );
		TimeUnit.MILLISECONDS.sleep(100);
		String t2 = Instant.now().toString();
		System.out.println( board.handleRequest(placeReq3) );
		TimeUnit.MILLISECONDS.sleep(100);
		String t3 = Instant.now().toString();
		
		
		String update2 = "{\"lastUpdate\":\""+t1+"\"}";
		String updateReq2 = "{\"type\":\"update\", \"msg\": "+update2+"  }";

		String update3 = "{\"lastUpdate\":\""+t2+"\"}";
		String updateReq3 = "{\"type\":\"update\", \"msg\": "+update3+"  }";
		
		String update4 = "{\"lastUpdate\":\""+t3+"\"}";
		String updateReq4 = "{\"type\":\"update\", \"msg\": "+update4+"  }";
		System.out.println(updateReq4);
		System.out.println(placeReq1);
		System.out.println("*****");
		System.out.println( board.handleRequest(updateReq2) );
		System.out.println( board.handleRequest(updateReq3) );
		System.out.println( board.handleRequest(updateReq4) );
		System.out.println( board.handleRequest(updateReq1) );
		
		board.updateChanges();
		board.printBoard();
	}
*/	
	
	public PlaceBoard(int h, int w){
		height = h;
		width = w;
		data = new int[h][w];
		changes = new Vector<PixelChange>();
		trimSize = 500;
		oldest = Instant.now();
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				data[i][j] = 0;
			}
		}
	}
	
	public void updateChanges(){
		//Remove older elements until maxSize is reached.
		while(changes.size() > trimSize){
			changes.removeElementAt(0);
		}
		if(changes.size() > 0){
			oldest = changes.get(0).stamp;
		}
	}

		
	public String handleRequest(String s){
		JSONParser parser = new JSONParser();
		String returnVal = "";
		String type,msg;
		try{
			JSONObject obj = (JSONObject)parser.parse(s);
			type = obj.get("type").toString();
			msg = obj.get("msg").toString();
			switch(type){
				case "update":
					returnVal = handleUpdate( msg );
					break;
				case "place":
					returnVal = handlePlacement( msg );
					break;
				default:
					System.out.println("Invalid Request");
					returnVal = "";
			}
		}catch(Exception pe){
			System.out.println("Bad String");
			System.out.println(pe);
			returnVal = "";
		}
		return returnVal;
	}

	
	public void printBoard(){
		int i,j;
		for(i=0;i<height;i++){
			for(j=0;j<width;j++){
				System.out.print(data[i][j]);
			}
			System.out.println();
		}
	}	
	
	public void printChanges(){
		int i;
		PixelChange pc;
		System.out.println("############");
		for(i=0;i<changes.size();i++){
			pc = changes.get(i);
			int x = pc.x;
			int y = pc.y;
			int c = pc.color;
			Instant inst = pc.stamp;
			String str = "" + inst.toString() + ", x:"+x+", y:"+y+", color:"+c;
			System.out.println(str);
		}
		System.out.println("############");
	}
		
	public String handleUpdate(String s){
		JSONParser parser = new JSONParser();
		Instant lastUpdate;
		Instant now = Instant.now();
		String updateContent = "";
		String updateType = "";
		try{
			JSONObject obj = (JSONObject)parser.parse(s);
			lastUpdate = Instant.parse( obj.get("lastUpdate").toString() );
			if(lastUpdate.isAfter(oldest)){
				// Send relevant changes
				updateType = "changes";
				updateContent += getRelevantChanges(lastUpdate);
			}else{
				// Send full board
				updateType = "refresh";
				updateContent += getFullBoard();
			}
		}catch(Exception pe){
			System.out.println("Failed!");
			System.out.println(pe);
			return "";
		}

		String response = "{\"updateType\":\""+updateType+"\", \"updateContent\":"+updateContent+", \"updateTime\":\""+now.toString()+"\" }";
		return response;
	}
	
	public String getFullBoard() {
		String boardContents="[";
		int x,y;
		for(y=0;y<height;y++){
			boardContents+="[";
			for(x=0;x<width;x++){
				boardContents += data[y][x];
				if(x!=width-1){
					boardContents+=",";
				}
			}
			boardContents+="]";
			if(y!=height-1){
				boardContents+=",";
			}
		}
		boardContents+="]";
		return "{\"boardHeight\":"+height+", \"boardWidth\":"+width+", \"board\": "+boardContents+" }";
	}

	public String getRelevantChanges(Instant lastUpdate) {
		String response = "[";
		int i=changes.size()-1;
		while( i>= 0 && changes.get(i).stamp.isAfter(lastUpdate) ){
			if(i!=changes.size()-1){
				response+=",";
			}
			response += changes.get(i).toJson();			
			i--;
		}
		response += "]";
		return response;
	}

	public String handlePlacement(String s){
		JSONParser parser = new JSONParser();
		int x,y,color;
		try{
			JSONObject obj = (JSONObject)parser.parse(s);
			x = Integer.parseInt(obj.get("x").toString());
			y = Integer.parseInt(obj.get("y").toString());
			color = Integer.parseInt(obj.get("color").toString());
			placePixel(color, x,y);
			return "";
		}catch(Exception pe){
			System.out.println("Failed!");
			System.out.println(pe);
			return "";
		}
	}
	
	public void placePixel(int color, int x, int y){
		PixelChange pc = new PixelChange(x,y,color, Instant.now());
		changes.add(pc);
		data[y][x] = color;
	}

	public class PixelChange{
		public Instant stamp;
		public int x,y,color;
		public PixelChange(int x, int y, int color, Instant stamp){
			this.stamp = stamp;
			this.x = x;
			this.y = y;
			this.color = color;
		}
		
		@SuppressWarnings("unchecked")
		public String toJson(){
			JSONObject obj = new JSONObject();
			obj.put("x", new Integer(x) );
			obj.put("y", new Integer(y));
			obj.put("color", new Integer(color));
			return obj.toJSONString();
		}
	}
}