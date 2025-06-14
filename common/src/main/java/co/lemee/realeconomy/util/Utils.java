/**
 * This file is part of RealEconomy.
 * <p>
 * RealEconomy is free software: you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * <p>
 * RealEconomy is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package co.lemee.realeconomy.util;

import co.lemee.realeconomy.ErrorManager;
import co.lemee.realeconomy.config.Config;
import co.lemee.realeconomy.currency.Currency;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Class for utility methods.
 */
public abstract class Utils {

    // Base path for the mods folder.
    public static final String BASE_PATH = new File("").getAbsolutePath() + "/config/RealEconomy/";

    /**
     * Method to write some data to file.
     * @param filePath the directory to write the file to
     * @param filename the name of the file
     * @param data the data to write to file
     * @return true if writing to file was successful
     */
    public static boolean writeFileAsync(String filePath, String filename, String data) {
        try {
            Path path = Paths.get(BASE_PATH + filePath + filename);

            // If the path doesn't exist, create it.
            if (!Files.exists(Paths.get(BASE_PATH + filePath))) {
                Files.createDirectory(Path.of(BASE_PATH + filePath));
            }

            // Write the data to file.
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put(data.getBytes());
            buffer.flip();

            fileChannel.write(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.clear();
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    exc.printStackTrace();
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Method to read a file asynchronously
     * @param filePath the path of the directory to find the file at
     * @param filename the name of the file
     * @param callback a callback to deal with the data read
     * @return true if the file was read successfully
     */
    public static boolean readFileAsync(String filePath, String filename, Consumer<String> callback) {
        try {
            Path path = Paths.get(BASE_PATH + filePath + filename);

            // If the directory doesn't exist, return false.
            if (!Files.exists(Paths.get(BASE_PATH + filePath))) {
                return false;
            }

            // Read the file.
            AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.READ);
            ByteBuffer buffer = ByteBuffer.allocate(1024);

            fileChannel.read(buffer, 0, buffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                    attachment.flip();
                    byte[] data = new byte[attachment.limit()];
                    attachment.get(data);
                    callback.accept(new String(data));
                    attachment.clear();
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                    exc.printStackTrace();
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Synchronous read method for json files.
     * @param subpath The path to add to the base path.
     * @param filename The name of the file to read.
     * @param dataType The class the file should be parsed to.
     * @return The object with the read data.
     */
    public static <T> T readFromFile(String subpath, String filename, Class<T> dataType) {
        try {
            File dir = checkForDirectory(BASE_PATH + subpath);

            String[] list = findFileName(dir, filename + ".json");

            // If no file exists, return null.
            if (list.length == 0) {
                return null;
            }

            File file = new File(dir, filename + ".json");
            Gson gson = newGson();

            Reader reader = new FileReader(file);

            // Read the file and parse it to an object.
            T data = gson.fromJson(reader, dataType);

            reader.close();

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Synchronous method to write an object as json.
     * @param subpath The path to add to the base path.
     * @param filename The name of the file.
     * @param data The data to write to the file.
     * @return true if the file was successfully written.
     */
    public static boolean writeToFile(String subpath, String filename, Object data) {
        try {
            File dir = checkForDirectory(BASE_PATH + subpath);

            String[] list = findFileName(dir, filename + ".json");

            File file = new File(dir, filename + ".json");
            Gson gson = newGson();

            // If the file doesn't exist, create it.
            if (list.length == 0) {
                file.createNewFile();
            }

            Writer writer = new FileWriter(file, false);

            // Parse the object to json and write to file.
            gson.toJson(data, writer);
            writer.flush();
            writer.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Method to check if a directory exists. If it doesn't, create it.
     * @param path The directory to check.
     * @return the directory as a File.
     */
    public static File checkForDirectory(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    /**
     * Finds a file from a directory.
     * @param dir the directory to check for the file.
     * @param filename the file to check for.
     * @return A list of files that match the filename.
     */
    private static String[] findFileName(File dir, String filename) {
        return dir.list((dir1, name) -> name.equals(filename));
    }

    /**
     * Method to create a new gson builder.
     * @return Gson instance.
     */
    public static Gson newGson() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    /**
     * Method to check the config is valid.
     * @return true if the config is valid.
     */
    public static boolean checkConfig(Config cfg) {
        ArrayList<Currency> currencies = cfg.getCurrencies();
        String defaultCurrency = cfg.getDefaultCurrency().trim().toLowerCase();

        for (int i = 0; i < currencies.toArray().length; i++) {
            String currentCurrency = currencies.get(i).getName();

            // Checks for currencies with the same name. (Duplicate currencies)
            for (int x = i + 1; x < currencies.toArray().length; x++) {
                String comparedCurrency = currencies.get(x).getName();
                if (currentCurrency.equals(comparedCurrency)) {
                    ErrorManager.addError("Found duplicate currency with name: " + currentCurrency);
                    return false;
                }
            }
        }

        // Checks that the default currency exists.
        for (Currency currency : currencies) {
            String currencyFormatted = currency.getName().trim().toLowerCase();
            if (currencyFormatted.equals(defaultCurrency)) {
                return true;
            }
        }
        // If the default currency doesn't exist, create an error.
        ErrorManager.addError("Multicurrency default currency " + cfg.getDefaultCurrency() +
                " doesn't match any existing currency name.");

        return false;
    }

    /**
     * Formats a message by removing minecraft formatting codes if sending to console.
     * @param message The message to format.
     * @param isPlayer If the sender is a player or console.
     * @return String that is the formatted message.
     */
    public static String formatMessage(String message, Boolean isPlayer) {
        if (isPlayer) {
            return message.trim();
        } else {
            return message.replaceAll("§[0-9a-fk-or]", "").trim();
        }
    }

    /**
     * Checks if a string can be parsed to integer.
     * @param string the string to try and parse.
     * @return true if the string can be parsed.
     */
    public static boolean isStringInt(String string) {
        try {
            Integer.parseInt(string);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
