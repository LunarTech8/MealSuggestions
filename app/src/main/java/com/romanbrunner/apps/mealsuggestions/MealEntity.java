package com.romanbrunner.apps.mealsuggestions;

import java.util.LinkedList;
import java.util.Objects;


public class MealEntity implements Meal
{
    // --------------------
    // Functional code
    // --------------------

    private final String name;
    private final LinkedList<Ingredient> ingredients;
    private final double sizeFactor;

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public double getSizeFactor()
    {
        return sizeFactor;
    }

    MealEntity(String name, LinkedList<Ingredient> ingredients, float sizeFactor)
    {
        this.name = name;
        this.ingredients = ingredients;
        this.sizeFactor = sizeFactor;
    }

    static boolean isContentTheSame(Meal mealA, Meal mealB)
    {
        return Objects.equals(mealA.getName(), mealB.getName())
            && Double.compare(mealA.getSizeFactor(), mealB.getSizeFactor()) == 0;
    }
}