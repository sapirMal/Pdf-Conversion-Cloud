import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.tools.PDFText2HTML;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class utilities {

    public static void toImg(String filename) throws IOException {
        try {
            PDDocument document = PDDocument.load(new File(filename+".pdf"));

            //System.out.println("toImg file name =========> "+ filename);

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage bI = renderer.renderImageWithDPI(0, 300, ImageType.RGB);
            ImageIOUtil.writeImage(bI, filename + ".png", 300);
            document.close();
        }
        catch(IOException e){
            throw new IOException("PDF to image failed");
        }

    }
    public static void toHtml(String filename) throws IOException {
        try {
            PDDocument document = PDDocument.load(new File(filename+".pdf"));

            //System.out.println("toHtml file name =========> "+ filename);


            PDFText2HTML stripper = new PDFText2HTML();
            stripper.setStartPage(1);
            stripper.setEndPage(2);
            String text = stripper.getText(document);
            text = fixHyperLinks(text, false);

            File f = new File(filename +".html");


            FileUtils.writeStringToFile(f, text, "UTF-8");
            document.close();
        }
        catch (IOException e){
            throw new IOException("PDF to HTML failed");

        }
    }
    public static void toText(String filename) throws IOException {
        try {
            PDDocument document = PDDocument.load(new File(filename+".pdf"));

            //System.out.println("toText file name =========> "+fname);


            PDFTextStripper tStripper = new PDFTextStripper();
            tStripper.setStartPage(0);
            tStripper.setEndPage(1);
            String text = tStripper.getText(document);
            PrintWriter pw = new PrintWriter(new FileWriter(filename + ".txt"));
            pw.write(text);
            pw.close();
            document.close();
        }
        catch(IOException e){
            throw new IOException("PDF to text failed");
        }
    }
    public static String extractFileName(String path){
        String[] parts = path.split("/");
        String toSplit = parts[parts.length-1];
        //System.out.println("toSplit ="+toSplit);
        String[] parts2 = toSplit.split("\\.");
        //System.out.println("parts2 = "+Arrays.toString(parts2));
        StringBuilder accBuilder = new StringBuilder();
        for(int i = 0; i<parts2.length - 1; i++){
            accBuilder.append(parts2[i]);
            if(i != parts2.length - 2) {
                accBuilder.append(".");
            }
        }
        //System.out.println("extracted file name:"+acc);
        return accBuilder.toString();
    }

    public static void stringToText(String filename, String outputText) throws IOException {
        try{
            PrintWriter pw = new PrintWriter(filename+".txt");
            pw.println(outputText);
            pw.close();
        }
        catch (IOException e){
            throw new IOException("PW fail");
        }
    }

    public static void stringToHTML(String filename, String content){
        String upperHalf =  "<html lang=\"en\">\n" +
                "<head>\n" +
                "<meta charset=\"utf-8\">\n" +
                "<title>Summary</title>\n" +
                "</head>\n" +
                "<body>\n";
        String lowerHalf =  "</body>\n" +
                "</html>";
        content = fixHyperLinks(content, false);
        content = content.replaceAll("ToHTML", "<br>ToHTML");
        content = content.replaceAll("ToImage", "<br>ToImage");
        content = content.replaceAll("ToText", "<br>ToText");

        content = content.substring(4);
        content = upperHalf + content;
        content = content + lowerHalf;
        try {
            PrintWriter pw = new PrintWriter(filename+".html");
            String[] lines = content.split("\n");
            for(String line : lines) {
                pw.println(line);
            }
            pw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    public static String fixHyperLinks(String input, boolean cleanText){
        String str = "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:\'\".,<>?«»“”‘’]))";
        Pattern patt = Pattern.compile(str);
        if(!cleanText)
            input = input.substring(input.indexOf('>')+1);
        Matcher matcher = patt.matcher(input);
        return matcher.replaceAll("<a href=\"$1\">$1</a>");
    }

}

