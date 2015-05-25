package com.words.controller.definition;

import com.words.controller.utils.Utils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Download word definition using WORD protocol.
 * @author vlad
 */
public class DictOrgDefinition extends AutomaticDefinition {
    
    private static final String HOST = "dict.org";
    private static final int PORT = 2628;
    private static final String DICTIONARY = "wn";
    
    @Override
    protected String downloadDefinition(String word) {
        word = Utils.normalizeFor3rdParties(word);
        
        try (Socket socket = new Socket(HOST, PORT);
            BufferedReader in = new BufferedReader(new InputStreamReader(
                socket.getInputStream(), StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout(TIMEOUT_MILLIS);
            
            String line;
            
            line = in.readLine();
            if (line != null && !line.startsWith("220"))
                return null;
           
            out.write(String.format("DEFINE %s \"%s\"%n", DICTIONARY, word));
            out.flush();
            
            line = in.readLine();
            if (!line.startsWith("150")) return null;
            in.readLine(); // trim useless info with some code
            in.readLine(); // trim word since we already know it
            
            StringBuilder sb = new StringBuilder();
            while ((line = in.readLine()) != null) {
                if (line.equals(".")) break;
                sb.append(line).append("\n");
            }
            sb.deleteCharAt(sb.length() - 1); // delete trailing space
            in.readLine();
            
            out.write("q\n");
            out.flush();
            
            return sb.toString().replaceAll(" ", "\u00A0").trim();
        } catch (SocketTimeoutException ste) {
            return null;
        } catch (IOException ex) {
            return null;
        }
    }
    
    public static void main(String[] args) {
        DictOrgDefinition d = new DictOrgDefinition();
        System.out.println(d.getDefinition("uneasiness"));
        System.out.println(d.getDefinition("designation"));
        System.out.println(d.getDefinition("fallacy"));
        System.out.println(d.getDefinition("shear"));
        System.out.println(d.getDefinition("1"));
    }
}
