package nju.software.downloader.util;

public class FileUtil {
    public static String increaseFileName(String filename,int index){
        int ext_index = filename.lastIndexOf('.' );
        if(ext_index==-1){
            return filename+"("+index+")" ;
        }
        filename = filename.substring(0,ext_index)+"("+index+")"+filename.substring(ext_index) ;
        return filename ;
    }
}
