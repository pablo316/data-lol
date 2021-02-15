package com.data.lol;

import java.io.IOException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        String flag="y",summoner, champion, patch="vacio", apiKey="vacio";
        Data data= new Data();
        Scanner input = new Scanner(System.in);
        while(flag.equals("y")){
            System.out.print("Obtener datos (y: para continuar, n:para salir): ");
            flag = input.next();
            if(flag.equals("y")){
                if(apiKey.equals("vacio") && patch.equals("vacio")){
                    System.out.print("apiKey: ");
                    apiKey = input.next();
                    System.out.print("patch: ");
                    patch = input.next();
                }
                System.out.print("summoner: ");
                summoner = input.next();
                System.out.print("champion: ");
                champion = input.next();
                data.datos(summoner,champion,apiKey,patch);
            }
        }
        System.out.println("Adios");
    }
}
