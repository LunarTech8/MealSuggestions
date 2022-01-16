package com.romanbrunner.apps.mealsuggestions;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.mealsuggestions.databinding.MealBinding;

import java.util.ArrayList;
import java.util.List;


class MealsAdapter extends RecyclerView.Adapter<MealsAdapter.EntryViewHolder>
{
    // --------------------
    // Functional code
    // --------------------

    private final MainActivity mainActivity;
    private List<? extends Meal> meals;

    static class EntryViewHolder extends RecyclerView.ViewHolder
    {
        final MealBinding binding;

        EntryViewHolder(MealBinding binding, MealsAdapter mealsAdapter)
        {
            super(binding.getRoot());
            binding.setUserMode(mealsAdapter.mainActivity.userMode);
            binding.multiplierButton.setOnClickListener((View view) ->
            {
                final int position = getBindingAdapterPosition();
                mealsAdapter.meals.get(position).incrementMultiplier();
                mealsAdapter.mainActivity.refreshData(false);
                mealsAdapter.notifyItemChanged(position);
            });
            binding.removeButton.setOnClickListener((View view) ->
            {
                final int position = getBindingAdapterPosition();
                mealsAdapter.mainActivity.removeMeal((MealEntity) mealsAdapter.meals.get(position));
                mealsAdapter.mainActivity.refreshData(false);
                mealsAdapter.meals.remove(position);
                mealsAdapter.notifyItemRemoved(position);
            });
            this.binding = binding;
        }
    }

    MealsAdapter(MainActivity mainActivity)
    {
        this.mainActivity = mainActivity;
        meals = null;
    }

    void setMeals(@NonNull final List<? extends Meal> newMeals)
    {
        if (meals == null)
        {
            // Add all entries:
            meals = new ArrayList<>(newMeals);
            notifyItemRangeInserted(0, newMeals.size());
        }
        else
        {
            Log.d("setMeals", "setMeals run");
            // Update changed entries:
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback()
            {
                @Override
                public int getOldListSize()
                {
                    return meals.size();
                }

                @Override
                public int getNewListSize()
                {
                    return newMeals.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return meals.get(oldItemPosition).getName().equals(newMeals.get(newItemPosition).getName());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition)
                {
                    return MealEntity.isContentTheSame(newMeals.get(newItemPosition), meals.get(oldItemPosition));
                }
            });
            meals = new ArrayList<>(newMeals);
            result.dispatchUpdatesTo(this);
        }
    }

    @Override
    public int getItemCount()
    {
        return (meals == null ? 0 : meals.size());
    }

    @Override
    public long getItemId(int position)
    {
        return meals.get(position).getName().hashCode();
    }

    @Override
    public @NonNull EntryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType)
    {
        MealBinding binding = DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.article, viewGroup, false);
        return new EntryViewHolder(binding, this);
    }

    @Override
    /* Is called when an item is reloaded (that was previously not visible) */
    public void onBindViewHolder(EntryViewHolder exerciseViewHolder, int position)
    {
        // Adjust changeable values of the view fields by the current entries list:
        final Meal meal = meals.get(position);
        exerciseViewHolder.binding.setMeal(meal);
        if (meal.isAvailable())
        {
            exerciseViewHolder.binding.name.setTextColor(Color.BLACK);
            exerciseViewHolder.binding.data.setTextColor(Color.BLACK);
        }
        else
        {
            exerciseViewHolder.binding.name.setTextColor(Color.GRAY);
            exerciseViewHolder.binding.data.setTextColor(Color.GRAY);
        }
        exerciseViewHolder.binding.setUserMode(mainActivity.userMode);
        exerciseViewHolder.binding.executePendingBindings();
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView)
    {
        super.onAttachedToRecyclerView(recyclerView);
    }
}