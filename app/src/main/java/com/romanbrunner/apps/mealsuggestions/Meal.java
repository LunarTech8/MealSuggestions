package com.romanbrunner.apps.mealsuggestions;


public interface Meal
{
    String getName();
    int getPortions();
    int getMultiplier();
    int getSelectionsLeft();

    void setName(String name);
    void setPortions(int portions);
    void setMultiplier(int multiplier);
    void setSelectionsLeft(int selectionsLeft);

    boolean isAvailable();
    void incrementMultiplier();
    void decrementSelectionsLeft();
    void markAsEmpty();
}