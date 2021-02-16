package com.data.lol;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class Data {
    private String urlMatchId="https://la2.api.riotgames.com/lol/match/v4/matches/";
    private String urlDdragon="http://ddragon.leagueoflegends.com/cdn/";
    private String urlByName="https://la2.api.riotgames.com/lol/summoner/v4/summoners/by-name/";
    private String urlMatchList="https://la2.api.riotgames.com/lol/match/v4/matchlists/by-account/";

    public void datos(String summoner, String champion, String apiKey, String patch) throws IOException {
        JsonArray dataMatches = new JsonArray();
        JsonObject dataItems = new JsonObject();
        JsonObject dataChampions= new JsonObject();
        ArrayList<Summoner> listMatches= new ArrayList<>();
        dataChampions=getChampion(patch);
        Integer id=dataChampions.get(champion).getAsJsonObject().get("key").getAsInt();
        String accountId =getSummonerId(summoner, apiKey);
        ArrayList<Integer> matchesId= getMatchList(accountId,apiKey, id);
        for (int i=0;i<matchesId.size();i++){
            dataMatches.add(getMatches(matchesId.get(i),apiKey));
        }
        if(dataMatches.size()>0){
            listMatches=summonerMatches(dataMatches,summoner, champion,dataChampions);
            writeExcel(listMatches,summoner,champion);
            System.out.println("Archivo creado");
        }
    }

    public ArrayList<Summoner> summonerMatches(JsonArray data,  String name,  String champ,JsonObject champions){
        ArrayList<Summoner> summonerList = new ArrayList<>();
        Integer participantId=0;
        String role="";
        String lane="";
        String rival="";
        JsonObject json = new JsonObject();
        JsonObject aux= new JsonObject();
        for (int i=0; i< data.size();i++){
            Summoner summoner = new Summoner();
            String url="";
            String build="";
            json=data.get(i).getAsJsonObject();
            for(int j=0;j<json.get("participantIdentities").getAsJsonArray().size();j++){
                if(json.get("participantIdentities").getAsJsonArray().get(j).getAsJsonObject().get("player").getAsJsonObject().get("summonerName").getAsString().equals(name)){
                    participantId=json.get("participantIdentities").getAsJsonArray().get(j).getAsJsonObject().get("participantId").getAsInt();
                }
            }
            //Obtener datos del jugador en la partida
            aux=json.get("participants").getAsJsonArray().get(participantId - 1).getAsJsonObject();
            role=aux.get("timeline").getAsJsonObject().get("role").getAsString();
            lane=aux.get("timeline").getAsJsonObject().get("lane").getAsString();

            if(aux.get("stats").getAsJsonObject().get("win").getAsBoolean()){
                summoner.setResult("Victoria");
            }else{
                summoner.setResult("Derrota");
            }
            url="https://blitz.gg/lol/match/la2/" + name + "/" + json.get("gameId").getAsInt();
            summoner.setUrl(url);
            summoner.setKda(aux.get("stats").getAsJsonObject().get("kills").getAsString() + "/" + aux.get("stats").getAsJsonObject().get("deaths").getAsString() + "/" + aux.get("stats").getAsJsonObject().get("assists").getAsString());
            summoner.setChampName(champ);
            rival=getChampionName(champions,role,lane,json.get("participants").getAsJsonArray(),participantId);
            summoner.setRivalName(rival);
            summonerList.add(summoner);
        }

        return summonerList;
    }

    public String getChampionName(JsonObject data, String role, String lane, JsonArray players, Integer id){
        String name="";
        Integer champId=0;
        for(int i=0;i< players.size();i++){
            if(players.get(i).getAsJsonObject().get("participantId").getAsInt() != id ){
                if(players.get(i).getAsJsonObject().get("timeline").getAsJsonObject().get("role").getAsString().equals(role) && players.get(i).getAsJsonObject().get("timeline").getAsJsonObject().get("lane").getAsString().equals(lane)){
                    champId=players.get(i).getAsJsonObject().get("championId").getAsInt();
                }
            }
        }
        Iterator<String> iterator = data.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if(data.get(key).getAsJsonObject().get("key").getAsInt() == champId){
                name=key;
                break;
            }

        }
        return name;
    }

    public JsonObject getChampion(String patch) throws IOException {
        JsonParser parser = new JsonParser();
        JsonObject json = new JsonObject();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(urlDdragon + patch + "/data/es_AR/champion.json")
                .method("GET", null)
                .build();
        Response response = null;
        response = client.newCall(request).execute();
        json = parser.parse(response.body().string()).getAsJsonObject().get("data").getAsJsonObject();
        return json;
    }

    public JsonObject getItems(String patch) throws IOException {
        JsonObject json = new JsonObject();
        JsonParser parser = new JsonParser();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(urlDdragon + patch + "/data/es_AR/item.json")
                .method("GET", null)
                .build();
        Response response = null;
        response = client.newCall(request).execute();
        json=parser.parse(response.body().string()).getAsJsonObject().get("data").getAsJsonObject();
        return json;
    }

    public String getSummonerId(String summoner, String apiKey) throws IOException {
        JsonParser parser = new JsonParser();
        String accountId="";
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        Request request = new Request.Builder()
                .url(urlByName + summoner + "?api_key=" + apiKey)
                .method("GET", null)
                .build();
        Response response = null;
        response = client.newCall(request).execute();
        if(response.code() == 200){
            accountId=parser.parse(response.body().string()).getAsJsonObject().get("accountId").getAsString();
        }else{
            System.out.println("Error: Nombre de invocador incorrecto o Error de Red");
        }
        return accountId;

    }

    public ArrayList<Integer> getMatchList(String accountId, String apiKey, Integer id) throws IOException {
         JsonParser parser = new JsonParser();
         ArrayList<Integer> array=new ArrayList();
         OkHttpClient client = new OkHttpClient().newBuilder()
                 .build();
         Request request = new Request.Builder()
                 .url(urlMatchList + accountId + "?champion=" + id + "&queue=420&endIndex=30&api_key=" + apiKey)
                 .method("GET", null)
                 .build();
         Response response = null;
         response = client.newCall(request).execute();
         if(response.code() == 200){
             JsonArray json=parser.parse(response.body().string()).getAsJsonObject().get("matches").getAsJsonArray();
             for(int i=0; i<json.size();i++){
                 array.add(json.get(i).getAsJsonObject().get("gameId").getAsInt());
             }
         }else{
             System.out.println("Error: No hay partidas en SoloQ con ese Campeón o Error de Red");
         }
        return array;
    }

    public JsonObject getMatches(Integer id, String apiKey) throws IOException {
         JsonParser parser = new JsonParser();
         OkHttpClient client = new OkHttpClient().newBuilder()
                 .build();
         Request request = new Request.Builder()
                 .url(urlMatchId + id + "?api_key=" + apiKey)
                 .method("GET", null)
                 .build();
         Response response = null;
         response = client.newCall(request).execute();
         JsonObject json=parser.parse(response.body().string()).getAsJsonObject();
         return json;
    }

    private static void writeExcel(ArrayList<Summoner> Data, String summoner, String champion) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        workbook.setSheetName(0, "Hoja excel");

        String[] headers = new String[]{
                "Campeón",
                "Enemigo",
                "Resultado",
                "KDA",
                "Url"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        HSSFRow headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; ++i) {
            String header = headers[i];
            HSSFCell cell = headerRow.createCell(i);
            cell.setCellStyle(headerStyle);
            cell.setCellValue(header);
        }

        for(int i =0; i<Data.size();i++){
            HSSFRow dataRow = sheet.createRow( i+1);
            dataRow.createCell(0).setCellValue(Data.get(i).getChampName());
            dataRow.createCell(1).setCellValue(Data.get(i).getRivalName());
            dataRow.createCell(2).setCellValue(Data.get(i).getResult());
            dataRow.createCell(3).setCellValue(Data.get(i).getKda());
            dataRow.createCell(4).setCellValue(Data.get(i).getUrl());
        }

        Calendar date = Calendar.getInstance();
        int mes=date.get(Calendar.MONTH)+1;

        FileOutputStream file = new FileOutputStream("docs/"+summoner + "-" + champion + "-" + Integer.toString(date.get(Calendar.DATE)) + "-" + mes +".xls");
        workbook.write(file);
        file.close();
    }

}
