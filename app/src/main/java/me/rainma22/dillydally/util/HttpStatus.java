package me.rainma22.dillydally.util;

public class HttpStatus {
    public static boolean isSuccess(int statusCode){
        return statusCode / 100 == 2;
    }
    
}
