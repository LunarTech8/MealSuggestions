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
    double getWeight();
    String getSpoonWeightString();
    double getSugarPercentage();
    String getSugarPercentageString();

    void setName(String name);
    void setBrand(String brand);
    void setType(Type type);
    void setWeight(double weight);
    void setSugarPercentage(double sugarPercentage);
}