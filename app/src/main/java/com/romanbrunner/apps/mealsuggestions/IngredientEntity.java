package com.romanbrunner.apps.mealsuggestions;

import java.util.Locale;
import java.util.Objects;


public class IngredientEntity implements Ingredient
{
    // --------------------
    // Functional code
    // --------------------

    private final Article article;
    private final double amount;

    @Override
    public String getName()
    {
        return article.getName();
    }

    @Override
    public String getBrand()
    {
        return article.getBrand();
    }

    @Override
    public double getAmount()
    {
        return amount;
    }

    @Override
    public String getSpoonCountString()
    {
        if (amount == 1)
        {
            return "1 " + article.getSpoonName();
        }
        else
        {
            return String.format(Locale.getDefault(), "%d " + article.getSpoonName() + "s", amount);
        }
    }

    @Override
    public String getWeightString()
    {
        return String.format(Locale.getDefault(), "%.1f", amount * article.getWeight());
    }

    @Override
    public String getSugarPercentageString()
    {
        return String.format(Locale.getDefault(), "%.1f", article.getSugarPercentage() * 100);
    }

    IngredientEntity(Article article, double amount)
    {
        this.article = article;
        this.amount = amount;
    }

    static boolean isContentTheSame(Ingredient ingredientA, Ingredient ingredientB)
    {
        return Objects.equals(ingredientA.getName(), ingredientB.getName())
            && ingredientA.getAmount() == ingredientB.getAmount();
    }
}