import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Pictures {
    static int numFiles = 0;
    static int modifiedFiles = 0;
    static String type1 = "-";
    static String type2 = " ";
    static String realType;

    private static int buscarUltimo(String fichero, ArrayList<String> ficheros){
        int result = 0;
        for (int j = 0; j < ficheros.size(); j++){
            String s = ficheros.get(j);
            String ext = obtenerExt(s);
            s = quitarExt(s);
            if (s.contains("_")){
                s = s.substring(0, s.indexOf("_"));
            }
            s += ext;
            if (s.equals(fichero)){
                result = j;
            }
        }
        return result;
    }

    private static String quitarExt(String fichero){
        int index = fichero.lastIndexOf(".");
        return fichero.substring(0, index);
    }

    private static String obtenerExt(String fichero){
        int index = fichero.lastIndexOf(".");
        return fichero.substring(index);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        LocalDate localDate1 = date1.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate localDate2 = date2.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate1.isEqual(localDate2);
    }

    private static void modifyInternalDate(File file, int y, int m, int d){
        // Boolean cambiarNombre = false;
        long lastModified = file.lastModified();
        Date lastModifiedDate = new Date(lastModified);
        SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
        SimpleDateFormat minFormat = new SimpleDateFormat("mm");
        SimpleDateFormat secFormat = new SimpleDateFormat("ss");
        int h = Integer.parseInt(hourFormat.format(lastModifiedDate));
        int min = Integer.parseInt(minFormat.format(lastModifiedDate));
        int s = Integer.parseInt(secFormat.format(lastModifiedDate));
        // System.out.println("Fecha de antes: " + lastModifiedDate + ". NOS QUEDAMOS CON: " + h + ":" + min + ":" + s);
        String newDate = y + "/" + m + "/" + d + " " + h + ":" + min + ":" + s;
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date newModifiedDate = null;
        try {
            newModifiedDate = format.parse(newDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        if (!isSameDay(Objects.requireNonNull(newModifiedDate), lastModifiedDate)){
            // if (newModifiedDate.after(lastModifiedDate)){
            //   cambiarNombre = true; NO TIENE SENTIDO ESTE CASO
            // }
            if (newModifiedDate.before(lastModifiedDate)){
                Date test = new Date(file.lastModified());
                file.setLastModified(newModifiedDate.getTime());
                Date test2 = new Date(file.lastModified());
                modifiedFiles++;
                System.out.println("#" + modifiedFiles + "(" + file.getName() + "). Antes: " + test + ". Después: " + test2);
            }
        } // Else es que tienen el mismo día y esta OK
        // return cambiarNombre;
    }


    private static void rename(final File carpeta, ArrayList<String> ficheros) throws IOException {
        for (final File ficheroEntrada : Objects.requireNonNull(carpeta.listFiles())) {
            if (ficheroEntrada.isDirectory()) {
                ArrayList<String> nuevo = new ArrayList<>();
                rename(ficheroEntrada, nuevo);
            } else {
                // Comprobar que es una imagen JPG/PNG/JPEG
                String mimetype = Files.probeContentType(ficheroEntrada.toPath());
                if (mimetype == null || !mimetype.split("/")[0].equals("image")) {
                    continue;
                }

                String oldName = ficheroEntrada.getName();
                String regex = "(\\d{4})" + realType + "(\\d{2})" + realType + "(\\d{2})(_\\d{3})?\\.\\w{3,4}";
                Pattern p2 = Pattern.compile(regex);
                Matcher m2 = p2.matcher(oldName);
                if (m2.matches()){
                    modifyInternalDate(ficheroEntrada, Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)),
                            Integer.parseInt(m2.group(3)));
                    ficheros.add(oldName);
                    continue;
                }

                // Obtener nombre viejo
                Pattern pattern = Pattern.compile("\\d{8}");
                Matcher matcher = pattern.matcher(oldName);
                String yOldName = "", mOldName = "", dOldName = "";

                // Obtener extension
                String extension = ficheroEntrada.toString().substring(ficheroEntrada.toString().lastIndexOf(".")).toLowerCase();
                // Obtener información del fichero
                // SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
                SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
                // SimpleDateFormat hourFormat = new SimpleDateFormat("HH");
                // SimpleDateFormat minFormat = new SimpleDateFormat("mm");
                // SimpleDateFormat secFormat = new SimpleDateFormat("ss");
                long dateFichero = ficheroEntrada.lastModified();

                String y = yearFormat.format(dateFichero);
                String m = monthFormat.format(dateFichero);
                String d = dayFormat.format(dateFichero);
                // String h = hourFormat.format(dateFichero);
                // String min = minFormat.format(dateFichero);
                // String s = secFormat.format(dateFichero);

                boolean noName = false;
                if (matcher.find()){
                    String regexName = matcher.group(0);
                    if (regexName.startsWith("20")){ // Del año 20XX, cambiar en el año 2100 xd
                        yOldName = regexName.substring(0, 4);
                        mOldName = regexName.substring(4, 6);
                        dOldName = regexName.substring(6, 8);
                    }
                } else if(oldName.contains("Screenshot")){ // Para el caso de capturas
                    yOldName = oldName.substring(11, 15);
                    mOldName = oldName.substring(16, 18);
                    dOldName = oldName.substring(19, 21);
                }
                else {
                    noName = true;
                }

                // Creando nombre con la fecha más antigua: o bien el nombre o el EXIF
                String fullOldName = yOldName + mOldName + dOldName;
                String nameExif = y + m + d;
                // Comparar fechas
                String renamed;
                if (!noName && fullOldName.compareTo(nameExif) < 1){ // Viejo: -1 o 0 es más viejo, mantenemos viejo
                    y = yOldName;
                    m = mOldName;
                    d = dOldName;
                    modifyInternalDate(ficheroEntrada, Integer.parseInt(y), Integer.parseInt(m), Integer.parseInt(d));
                }

                renamed = y + realType + m + realType + d + extension;

                if (ficheros.contains(renamed)){
                    int ultimo = buscarUltimo(renamed, ficheros);
                    String ult = ficheros.get(ultimo);
                    if (ult.contains("_")){
                        String aux = quitarExt(ult);
                        aux = aux.substring(aux.indexOf("_") +1 );
                        int n = Integer.parseInt(aux);
                        n++;
                        String formatted = String.format("%03d", n);
                        renamed = y + realType + m + realType + d + "_" + formatted + extension;
                    }
                    else{
                        renamed = y + realType + m + realType + d + "_00" + 1 + extension;
                    }
                }
                ficheros.add(renamed);
                if (!ficheroEntrada.getName().equals(renamed)){
                    numFiles++;
                    System.out.println(numFiles + ". Antes: " + ficheroEntrada.getName() + ". Ahora:" + renamed);
                    String finalPath = carpeta + "\\" + renamed;
                    File newFile = new File(finalPath);
                    ficheroEntrada.renameTo(newFile);
                }

            }
        }
    }

    private static void listarDirectorios(File folder){
        for (File ficheroEntrada : Objects.requireNonNull(folder.listFiles())){
            if (ficheroEntrada.isDirectory()){
                System.out.println(ficheroEntrada);
                listarDirectorios(ficheroEntrada);
            }
        }
    }
    public static void main(String[] args) throws IOException {
        System.out.println(" ****************** RENOMBRAR IMÁGENES ******************");
        System.out.println(" ********************************************************");
        System.out.println("\nHola!! Tienes la opción de elegir diferentes formas...");
        System.out.println("Opción 1) Formato guion: 2019-12-10 (para varias fotos en el mismo dia 2019-12-10_02)");
        System.out.println("Opción 2) Formato espacio: 2019 12 10 (para varias fotos en el mismo dia 2019 12 10_03)");
        System.out.println("Selección 1 o 2, por favor...");
        Scanner scan = new Scanner(System.in);
        String leido = scan.nextLine();
        boolean entradaOk = false;
        int type = 0;
        while (!entradaOk){
            try {
                type = Integer.parseInt(leido);
                if (type > 0 && type < 3) {
                    entradaOk = true;
                }
                else {
                    System.out.println("Elige una opción correcta anda... (1 o 2)");
                    leido = scan.nextLine();
                }
            } catch (Exception any){
                System.out.println("Eso no es ni un numero...");
                System.out.println("Co, elige una de las 2... (1 o 2)");
                leido = scan.nextLine();
            }
        }
        System.out.println("Has elegido opción: " + type);
        if (type == 1){
            realType = type1;
        }
        else {
            realType = type2;
        }
        System.out.println("Venga, dime ahora la ruta A PARTIR de la cual quieres hacer los renombres...");
        String path = scan.nextLine();
        System.out.println("Has elegido la \"" + path + "\", OJO!!! implica también sus subdirectorios");
        System.out.println("SEGURO 100%??... (si/no)");
        entradaOk = false;
        String okay = scan.nextLine();
        while (!entradaOk){
            try {
                if (okay.equals("si")) {
                    entradaOk = true;
                }
                else if (okay.equals("no")){
                    System.out.println("Menos mal que te he vuelto a preguntar... Dime otra ruta...");
                    path = scan.nextLine();
                    System.out.println("Has elegido la ruta: \"" + path + "\" SEGURO 100%??... (si/no)");
                    okay = scan.nextLine();
                }
                else {
                    System.out.println("Elige si o no, escribe tal cual \"si\" o \"no\"");

                    okay = scan.nextLine();
                }
            } catch (Exception any){
                System.out.println("Eso no es ni una opción...");
                System.out.println("Co, elige una \"si\" o \"no\"");
                okay = scan.nextLine();
            }
        }

        final File folder = new File(path);
        if (!folder.exists()){
            System.out.println("El directorio no existe... Vuelve a lanzarlo bien");
        }
        else {
            System.out.println("Ultima comprobación... Modificaras los nombres de las fotos de estas carpetas: ");
            System.out.println(folder);
            listarDirectorios(folder);
            System.out.println("VAMOS?! (si/no)");
            String go = scan.nextLine();
            if (go.equals("si")){
                System.out.println("Alla vamos");
                ArrayList<String> ficheros = new ArrayList<>();
                rename(folder, ficheros);
            }
            else if(go.equals("no")) {
                System.out.println("Menos mal que te he avisado una ultima vez...");
            }
            else{
                System.out.println("La opción solo era si/no...");
            }
        }
        System.out.println("TOTAL ARCHIVOS MODIFICADOS: " + numFiles);
        System.out.println("TOTAL FECHAS ACTUALIZADAS: " + modifiedFiles);
    }
}