package de.cas_ual_ty.ydm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.Files;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.cas_ual_ty.ydm.card.Card;
import de.cas_ual_ty.ydm.card.CustomCards;
import de.cas_ual_ty.ydm.card.properties.Properties;
import de.cas_ual_ty.ydm.util.DNCList;
import de.cas_ual_ty.ydm.util.YdmIOUtil;
import de.cas_ual_ty.ydm.util.YdmUtil;

public class YdmDatabase
{
    public static final DNCList<Long, Properties> PROPERTIES_LIST = new DNCList<>((p) -> p.getId(), Long::compare);
    public static final DNCList<String, Card> CARDS_LIST = new DNCList<>((c) -> c.getSetId(), (s1, s2) -> s1.compareTo(s2));
    
    public static final JsonParser JSON_PARSER = new JsonParser();
    
    public static void readFiles()
    {
        YDM.log("Reading database!");
        
        CustomCards.createAndRegisterEverything();
        
        if(!YDM.mainFolder.exists())
        {
            YDM.log(YDM.mainFolder.getAbsolutePath() + " does not exist! Aborting...");
            return;
        }
        
        if(!YDM.cardsFolder.exists())
        {
            YDM.log(YDM.cardsFolder.getAbsolutePath() + " does not exist! Aborting...");
            return;
        }
        
        YdmDatabase.readCards(YDM.cardsFolder);
        
        if(!YDM.setsFolder.exists())
        {
            YDM.log(YDM.setsFolder.getAbsolutePath() + " does not exist! Aborting...");
            return;
        }
        
        YdmDatabase.readSets(YDM.setsFolder);
    }
    
    public static void downloadDatabase() throws IOException
    {
        YDM.log("Downloading cards database from " + YDM.dbSourceUrl);
        
        URL url = new URL(YDM.dbSourceUrl);
        
        // archive containing the files
        File zip = new File("ydm_db_temp.zip");
        if(zip.exists())
        {
            zip.delete();
        }
        
        // archive to inpack to
        File temp = new File("ydm_db_temp");
        if(temp.exists())
        {
            temp.delete();
        }
        temp.mkdir();
        
        // download the zipped db
        YdmIOUtil.downloadFile(url, zip);
        
        // --- zip unpack ---
        
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zip));
        ZipEntry entry = zipIn.getNextEntry();
        
        byte[] buffer = new byte[1024];
        
        File currentFile;
        FileOutputStream zipOut;
        int length;
        
        while(entry != null)
        {
            currentFile = new File(temp, entry.getName());
            
            if(entry.isDirectory())
            {
                currentFile.mkdir();
            }
            else
            {
                zipOut = new FileOutputStream(currentFile);
                
                while((length = zipIn.read(buffer)) > 0)
                {
                    zipOut.write(buffer, 0, length);
                }
                
                zipOut.close();
            }
            
            entry = zipIn.getNextEntry();
        }
        
        zipIn.closeEntry();
        zipIn.close();
        
        zip.delete();
        
        // --- zip unpack end ---
        
        // now move the file out
        YdmIOUtil.doForDeepSearched(temp, (file) -> file.getName().equals(YDM.mainFolder.getName()), (file) ->
        {
            try
            {
                Files.move(file, YDM.mainFolder);
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
        
        // now delete temp folder
        YdmIOUtil.deleteRecursively(temp);
        
        YDM.log("Finished downloading cards database!");
    }
    
    private static void readCards(File cardsFolder)
    {
        YDM.log("Reading card files from: " + cardsFolder.getAbsolutePath());
        
        File[] cardsFiles = cardsFolder.listFiles(YdmIOUtil.JSON_FILTER);
        YdmDatabase.PROPERTIES_LIST.ensureExtraCapacity(cardsFiles.length);
        
        JsonObject j;
        Properties p;
        
        for(File cardFile : cardsFiles)
        {
            try
            {
                j = YdmIOUtil.parseJsonFile(cardFile).getAsJsonObject();
                p = YdmUtil.buildProperties(j);
                YdmDatabase.PROPERTIES_LIST.add(p);
            }
            catch (NullPointerException e)
            {
                YDM.log("Failed reading card: " + cardFile.getAbsolutePath());
                e.printStackTrace();
            }
            catch (JsonSyntaxException e)
            {
                YDM.log("Failed reading card: " + cardFile.getAbsolutePath());
                e.printStackTrace();
            }
            catch (JsonIOException | FileNotFoundException e)
            {
                YDM.log("Failed reading card: " + cardFile.getAbsolutePath());
                e.printStackTrace();
            }
            catch (IOException e)
            {
                YDM.log("Failed reading card: " + cardFile.getAbsolutePath());
                e.printStackTrace();
            }
            catch (Exception e)
            {
                YDM.log("Failed reading card: " + cardFile.getAbsolutePath());
                throw e;
            }
        }
        
        YdmDatabase.PROPERTIES_LIST.sort();
        
        YDM.log("Done reading card files!");
    }
    
    private static void readSets(File setsFolder)
    {
        YDM.log("Reading set files from: " + setsFolder.getAbsolutePath());
        
        //TODO
        
        YdmDatabase.CARDS_LIST.ensureExtraCapacity(YdmDatabase.PROPERTIES_LIST.size());
        for(Properties properties : YdmDatabase.PROPERTIES_LIST)
        {
            if(properties.getIsHardcoded())
            {
                continue;
            }
            
            for(byte imageIndex = 0; imageIndex < properties.images.length; ++imageIndex)
            {
                YdmDatabase.CARDS_LIST.add(new Card(properties, imageIndex));
            }
        }
        YdmDatabase.CARDS_LIST.sort();
        
        YDM.log("Done reading set files!");
    }
}
