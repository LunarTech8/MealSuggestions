package com.romanbrunner.apps.mealsuggestions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Objects;


public class MealEntity implements Meal
{
    // --------------------
    // Data code
    // --------------------

    private final static int MAX_MULTIPLIER = 3;
    private final static Charset STRING_CHARSET = StandardCharsets.UTF_8;
    private final static int BYTE_BUFFER_LENGTH_INT = 4;


    // --------------------
    // Functional code
    // --------------------

    private String name;
    private LinkedList<Ingredient> ingredients;
    private int portions;
    private int multiplier;  // Quantifier for how often the meal has to be chosen before it is used, 0 means unavailable
    private int selectionsLeft;  // Counter for how often the meal can still be chosen, 0 means it is used

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public int getPortions()
    {
        return portions;
    }

    @Override
    public int getMultiplier()
    {
        return multiplier;
    }

    @Override
    public int getSelectionsLeft()
    {
        return selectionsLeft;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public void setPortions(int portions)
    {
        this.portions = portions;
    }

    @Override
    public void setMultiplier(int multiplier)
    {
        this.multiplier = multiplier;
    }

    @Override
    public void setSelectionsLeft(int selectionsLeft)
    {
        this.selectionsLeft = selectionsLeft;
    }

    @Override
    public boolean isAvailable()
    {
        return multiplier > 0;
    }

    @Override
    public void incrementMultiplier()
    {
        if (multiplier >= MAX_MULTIPLIER)
        {
            multiplier = 0;
            selectionsLeft = 0;
        }
        else
        {
            multiplier += 1;
            selectionsLeft += 1;
        }
    }

    @Override
    public void decrementSelectionsLeft()
    {
        if (selectionsLeft > 0)
        {
            selectionsLeft -= 1;
        }
    }

    @Override
    public void markAsEmpty()
    {
        this.multiplier = 0;
    }

    MealEntity(String name, LinkedList<Ingredient> ingredients, int portions)
    {
        this.name = name;
        this.ingredients = ingredients;
        this.portions = portions;
        this.multiplier = 1;
        this.selectionsLeft = 1;
    }
    MealEntity(JSONObject jsonObject) throws JSONException
    {
        readFromJson(jsonObject);
        this.multiplier = 0;
        this.selectionsLeft = 0;
    }
    MealEntity(byte[] dataBytes) throws IOException
    {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(dataBytes);
        name = new String(readEntry(inputStream, inputStream.read()), STRING_CHARSET);
        // TODO: add ingredients
        portions = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_INT)).getInt();
        multiplier = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_INT)).getInt();
        selectionsLeft = ByteBuffer.wrap(readEntry(inputStream, BYTE_BUFFER_LENGTH_INT)).getInt();
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

    static boolean isNameTheSame(Meal mealA, Meal mealB)
    {
        return Objects.equals(mealA.getName(), mealB.getName());
    }

    static boolean isContentTheSame(Meal mealA, Meal mealB)
    {
        return Objects.equals(mealA.getName(), mealB.getName())
            && mealA.getPortions() == mealB.getPortions()
            && mealA.getSelectionsLeft() == mealB.getSelectionsLeft()
            && mealA.getMultiplier() == mealB.getMultiplier();
    }

    byte[] toByteArray() throws IOException
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writeEntry(outputStream, name.getBytes(STRING_CHARSET), true);
        // TODO: add ingredients
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_INT).putInt(portions).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_INT).putInt(multiplier).array(), false);
        writeEntry(outputStream, ByteBuffer.allocate(BYTE_BUFFER_LENGTH_INT).putInt(selectionsLeft).array(), false);
        return outputStream.toByteArray();
    }

    JSONObject writeToJson() throws JSONException
    {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("name", name);
        // TODO: add ingredients
        jsonObject.put("portions", portions);
        return jsonObject;
    }

    void readFromJson(JSONObject jsonObject) throws JSONException
    {
        this.name = jsonObject.getString("name");
        // TODO: add ingredients
        this.portions = jsonObject.getInt("portions");
    }
}