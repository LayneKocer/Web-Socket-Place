package OurPlace;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.*;
import java.util.Iterator;
import java.util.Vector;
//import java.util.concurrent.TimeUnit;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class PlaceBoard {
	
	private int height;
	private int width;
	// import Board data
	private int[][] data;
	private Vector<PixelChange> changes;
	private int trimSize;
	private Instant oldest;
	
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
	q
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

	public void loadBoard(){
		String content = null;
		try {
			content = readFile("C:/Users/John Galt/Workspace/JEE/HelloWebSocket/build/BoardState.json", Charset.defaultCharset());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// System.out.println(content);
		
		JSONParser parser = new JSONParser();
		JSONObject obj = null;
		JSONArray mapping = null;
		try {
			obj = (JSONObject)parser.parse(content);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		mapping = (JSONArray) obj.get("board");
		Iterator<JSONArray> iterator = mapping.iterator();
		for(int y=0; y<height && iterator.hasNext(); y++){
			JSONArray colors = iterator.next();
			Iterator<Long> colorLayer = colors.iterator();
			for(int x=0; x<width && colorLayer.hasNext(); x++){
				String tmp = colorLayer.next().toString();
				data[y][x] = Integer.parseInt(tmp);
			}
		}		
	}
	
	public void saveBoard(String boardState){
		JSONParser parser = new JSONParser();
		JSONObject obj = null;
		try {
			obj = (JSONObject)parser.parse(boardState);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boardState = obj.get("updateContent").toString();
		try{
		    PrintWriter writer = new PrintWriter("C:/Users/John Galt/Workspace/JEE/HelloWebSocket/build/BoardState.json");
		    writer.println(boardState);
		    writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static String readFile(String path, Charset encoding) 
		throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
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