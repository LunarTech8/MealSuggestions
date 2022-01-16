package com.romanbrunner.apps.mealsuggestions;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;


public class ArticleEntity implements Article
{
    // --------------------
    // Data code
    // --------------------

    private final static Charset STRING_CHARSET = StandardCharsets.UTF_8;
    private final static int BYTE_BUFFER_LENGTH_INT = 4;
    private final static int BYTE_BUFFER_LENGTH_DOUBLE = 8;

    public enum Type
    {
        FILLER, REGULAR, TOPPING;

        private static final Type[] values = Type.values();

        public static Type fromInt(int intValue)
        {
            if (intValue < 0 || intValue >= values.length)
            {
                Log.e("Type", "Invalid intValue (" + intValue + " has to be at least 0 and smaller than " + values.length + ")");
            }

            return values[intValue];
        }

        public @NotNull String toString()
        {
            if (this == FILLER)
            {
                return "Filler";
            }
            else if (this == REGULAR)
            {
                return "Regular";
            }
            else if (this == TOPPING)
            {
                return "Topping";
            }
            else
            {
                Log.e("Type", "Unrecognized type (" + this + ")");
                return "UNRECOGNIZED";
            }
        }

        public @NotNull String toSpoonName()
        {
            if (this == FILLER || this == REGULAR)
            {
                return "tablespoon";
            }
            else if (this == TOPPING)
            {
                return "teaspoon";
            }
            else
            {
                Log.e("Type", "Unrecognized type (" + this + ")");
                return "UNRECOGNIZED";
            }
        }
    }


    // --------------------
    // Functional code
    // --------------------

    private String name;
    private String brand;
    private Type type;
    private double weight;
    private double sugarPercentage;

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getBrand()
    {
        return brand;
    }

    @Override
    public Type getType()
    {
        return type;
    }

    @Override
    public String getTypeString()
    {
        return type.toString();
    }

    @Override
    public String getSpoonName()
    {
        return type.toSpoonName();
    }

    @Override
    public String getSpoonNameCapitalized()
    {
        return StringUtils.capitalize(type.toSpoonName());
    }

    @Override
    public double getWeight()
    {
        return weight;
    }

    @Override
    public String getSpoonWeightString()
    {
        return String.format(Locale.getDefault(), "%.1f", weight);
    }

    @Override
    public double getSugarPercentage()
    {
        return sugarPercentage;
    }

    @Override
    public String getSugarPercentageString()
    {
        return String.format(Locale.getDefault(), "%.1f", sugarPercentage * 100);
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    @Override
    public void setType(Type type)
    {
        this.type = type;
    }

    @Override
    public void setWeight(double weight)
    {
        this.weight = weight;
    }

    @Override
    public void setSugarPercentage(double sugarPercentage)
    {
        this.sugarPercentage = sugarPercentage;
    }

    ArticleEntity(String name, String brand, Type type, double weight, double sugarPercentage)
    {
        this.name = name;
        this.brand = brand;
        this.type = type;
        this.weight = weight;
        this.sugarPercentage = sugarPercentage;
    }
    ArticleEntity(JSONObject jsonObject) throws JSONException
    {
        readFromJson(jsonObject);
    }
    ArticleEntity(byte[] dataBytes) throws IOException
    {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes);
        name = new String(readEntry(inputStream, inputStream.read()), STRING_CHARSET);
        brand = new String(readEntry(inputStream, inputStream.read()), STRING_CHARSET);
        type = Type.fromInt(ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_INT)).getInt());
        weight = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_DOUBLE)).getDouble();
        sugarPercentage = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_DOUBLE)).getDouble();
    }

    private static byte[] readEntry(final ByteArrayInputStream inputStream, final int entryBytesLength) throws IOException
    {
        byte[] entryBytes = new byte[entryBytesLength];
        //noinspection ResultOfMethodCallIgnored
        inputStream.read(entryBytes);
        return entryBytes;
    }

    private static void writeEntry(final ByteArrayOutputStream outputStream, final byte[] entryBytes, final boolean storeLength) throws IOException
    {
        if (storeLength)
        {
            outputStream.write(entryBytes.length);
        }
        outputStream.write(entryBytes);
    }

    static boolean isNameTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand());
    }

    static boolean isContentTheSame(Article articleA, Article articleB)
    {
        return Objects.equals(articleA.getName(), articleB.getName())
            && Objects.equals(articleA.getBrand(), articleB.getBrand())
            && articleA.getType() == articleB.getType()
            && Double.compare(articleA.getWeight(), articleB.getWeight()) == 0
            && Double.compare(articleA.getSugarPercentage(), articleB.getSugarPercentage()) == 0;
    }

    byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeEntry(outputStream, name.getBytes(STRING_CHARSET), true);
        writeEntry(outputStream, brand.getBytes(STRING_CHARSET), true);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_INT).putInt(type.ordinal()).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_DOUBLE).putDouble(weight).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_DOUBLE).putDouble(sugarPercentage).array(), false);
        return outputStream.toByteArray();
    }

    JSONObject writeToJson() throws JSONException
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        jsonObject.put("brand", brand);
        jsonObject.put("type", type.ordinal());
        jsonObject.put("spoonWeight", weight);
        jsonObject.put("sugarPercentage", sugarPercentage);
        return jsonObject;
    }

    void readFromJson(JSONObject jsonObject) throws JSONException
    {
        this.name = jsonObject.getString("name");
        this.brand = jsonObject.getString("brand");
        this.type = Type.fromInt(jsonObject.getInt("type"));
        this.weight = jsonObject.getDouble("spoonWeight");
        this.sugarPercentage = jsonObject.getDouble("sugarPercentage");
    }
}