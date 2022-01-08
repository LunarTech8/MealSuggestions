package com.romanbrunner.apps.mealsuggestions;

import static com.romanbrunner.apps.mealsuggestions.ArticleEntity.*;


public interface Article
{
    String getName();
    String getBrand();
    Type getType();
    String getTypeString();
    String getSpoonName();
    String getSpoonNameCapitalized();
    double getSpoonWeight();
    String getSpoonWeightString();
    double getSugarPercentage();
    String getSugarPercentageString();
    int getMultiplier();
    int getSelectionsLeft();

    void setName(String name);
    void setBrand(String brand);
    void setType(Type type);
    void setSpoonWeight(double spoonWeight);
    void setSugarPercentage(double sugarPercentage);
    void setMultiplier(int multiplier);
    void setSelectionsLeft(int selectionsLeft);

    boolean isAvailable();
    void incrementMultiplier();
    void decrementSelectionsLeft();
}