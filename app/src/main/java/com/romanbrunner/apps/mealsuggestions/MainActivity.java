package com.romanbrunner.apps.mealsuggestions;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;

import com.romanbrunner.apps.mealsuggestions.databinding.MainScreenBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.Function;

import static com.romanbrunner.apps.mealsuggestions.ArticleEntity.*;


public class MainActivity extends AppCompatActivity
{
    // --------------------
    // Data code
    // --------------------

    private final static String INTENT_TYPE_JSON = "*/*";  // No MIME type for json yet, thus allowing every file
    private final static String ARTICLES_FILENAME = "AllArticles";
    private final static String PREFS_NAME = "GlobalPreferences";
    private final static String EXPORT_ITEMS_FILENAME = "MealItemsData";
    private final static int DEFAULT_ARTICLES_RES_ID = R.raw.default_muesli_items_data;

    private static float sizeValue2SizeWeight(int sizeValue)
    {
        return 35F + 15F * sizeValue;
    }


    // --------------------
    // Functional code
    // --------------------

    public enum UserMode
    {
        SUGGEST_MEAL, AVAILABILITY, EDIT_ITEMS
    }

    public UserMode userMode = UserMode.SUGGEST_MEAL;
    public boolean isChosenMealUsed = true;

    private final List<MealEntity> allMeals = new LinkedList<>();  // All catalogued meals, also not available ones
    private final List<MealEntity> selectableMeals = new LinkedList<>();  // Selectable meals for the next meal suggestion
    private final List<MealEntity> usedMeals = new LinkedList<>();  // Used meals, will be reshuffled into selectableMeals once that is depleted
    private MealEntity chosenMeal = null;  // Chosen meal for the current meal suggestion
    private IngredientsAdapter ingredientsAdapter;
    private MealsAdapter availableMealsAdapter;
    private MealSuggestion mealSuggestion;
    private MainScreenBinding binding;
    private int sizeValue;
    private String itemsJsonString;
    private ActivityResultLauncher<Intent> createFileActivityLauncher;
    private ActivityResultLauncher<Intent> openFileActivityLauncher;

    private static <T> double getLowestValue(final List<T> list, final Function<T, Double> getter)
    {
        if (list.isEmpty())
        {
            Log.e("getLowestValue", "Cannot get value for an empty list");
        }

        double lowestValue = getter.apply(list.get(0));
        for (int i = 1; i < list.size(); i++)
        {
            lowestValue = Math.min(lowestValue, getter.apply(list.get(i)));
        }
        return lowestValue;
    }

    public void removeMeal(final MealEntity meal)
    {
        allMeals.remove(meal);
    }

    private void addMealsToFittingStateList(final List<MealEntity> meals)
    {
        for (MealEntity meal: meals)
        {
            if (meal.isAvailable())
            {
                if (meal.getSelectionsLeft() == 0)
                {
                    usedMeals.add(meal);
                }
                else
                {
                    selectableMeals.add(meal);
                }
            }
        }
    }

    private void refreshCountInfo()
    {
        binding.setUsedAmount(usedMeals.size());
        int count = 0;
        for (MealEntity meal: selectableMeals) { count += meal.getSelectionsLeft(); }
        binding.setSelectableAmount(count);
    }

    private void storeArticles(final List<ArticleEntity> articles)
    {
        try
        {
            byte[] bytes;
            ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
            for (ArticleEntity article: articles)
            {
                bytes = article.toByteArray();
                dataOutputStream.write(bytes.length);
                dataOutputStream.write(bytes);

                if (bytes.length > 255)
                {
                    Log.e("onPause", "Data size of an Article is too big, consider limiting allowed string sizes or use two bytes for data size");
                }
            }
            dataOutputStream.close();
            FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(ARTICLES_FILENAME, Context.MODE_PRIVATE);
            fileOutputStream.write(dataOutputStream.toByteArray());
            fileOutputStream.close();
        }
        catch (IOException e)
        {
            Log.e("storeArticles", "Storing articles to " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
    }

    private boolean loadArticles(final List<ArticleEntity> articles)
    {
        try
        {
            final Context context = getApplicationContext();
            final List<String> fileNames = new ArrayList<>(Arrays.asList(context.fileList()));
            if (fileNames.contains(ARTICLES_FILENAME))
            {
                byte[] bytes;
                FileInputStream fileInputStream = context.openFileInput(ARTICLES_FILENAME);
                int length;
                while ((length = fileInputStream.read()) != -1)
                {
                    bytes = new byte[length];
                    //noinspection ResultOfMethodCallIgnored
                    fileInputStream.read(bytes);
                    articles.add(new ArticleEntity(bytes));
                }
                fileInputStream.close();
                return true;
            }
        }
        catch (IOException e)
        {
            Log.e("loadArticles", "Loading articles from " + ARTICLES_FILENAME + " failed");
            e.printStackTrace();
        }
        return false;
    }

    private void storePreferences()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("sizeValue", sizeValue);
        editor.apply();
    }

    private void loadPreferences()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(PREFS_NAME, 0);
        sizeValue = sharedPrefs.getInt("sizeValue", binding.sizeSlider.getProgress());
        binding.sizeSlider.setProgress(sizeValue);
    }

    private void hideKeyboard(final View view)
    {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        Objects.requireNonNull(inputMethodManager).hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void setEditTextFocus(final View view, final boolean hasFocus)
    {
        if (!hasFocus)
        {
            // Hide keyboard when tapping out of edit text:
            hideKeyboard(view);
        }
    }

    private void updateItemsJsonString()
    {
        try
        {
            final JSONArray jsonArray = new JSONArray();
            // TODO: also store articles
            for (MealEntity meal: allMeals)
            {
                jsonArray.put(meal.writeToJson());
            }
            itemsJsonString = jsonArray.toString(4);
        }
        catch (JSONException e)
        {
            Log.e("updateItemsJsonString", "Update of itemsJsonString to values of allArticles failed, setting it to an empty string");
            itemsJsonString = "";
            e.printStackTrace();
        }
    }

    private void mergeItemsJsonString()
    {
        try
        {
            final JSONArray jsonArray = new JSONArray(itemsJsonString);
            boolean hasNewItems = false;
            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                MealEntity newMeal = new MealEntity(jsonObject);
                // Add json entry as new meal if an entry with the same name doesn't exist already:
                if (allMeals.stream().noneMatch(meal -> isNameTheSame(meal, newMeal)))
                {
                    allMeals.add(newMeal);
                    addMealsToFittingStateList(Collections.singletonList(newMeal));
                    hasNewItems = true;
                }
            }
            if (hasNewItems)
            {
                allMeals.sort(Comparator.comparing(MealEntity::getName));
                availableMealsAdapter.setMeals(allMeals);
            }
        }
        catch (JSONException e)
        {
            Log.e("mergeItemsJsonString", "itemsJsonString is corrupted, resetting it to the values of allArticles");
            updateItemsJsonString();
            e.printStackTrace();
        }
    }

    private String readTextFile(final Uri targetUri) throws IOException
    {
        final InputStream inputStream = getContentResolver().openInputStream(targetUri);
        final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream)));
        final StringBuilder stringBuilder = new StringBuilder();
        String line = bufferedReader.readLine();
        while (line != null)
        {
            stringBuilder.append(line);
            line = bufferedReader.readLine();
        }
        bufferedReader.close();
        inputStream.close();
        return stringBuilder.toString();
    }

    private void writeTextFile(final Uri targetUri, final String text) throws IOException
    {
        final OutputStream outputStream = getContentResolver().openOutputStream(targetUri, "w");
        final BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(Objects.requireNonNull(outputStream)));
        bufferedWriter.write(text);
        bufferedWriter.close();
        outputStream.close();
    }

    private class MealSuggestion  // TODO: refactor to simple one item suggestions
    {
        private float targetPortions;  // TODO: maybe change to int
        private float totalWeight;
        private float totalSugar;
        private final List<IngredientEntity> ingredients;

        public MealSuggestion(final float targetPortions)
        {
            this.targetPortions = targetPortions;
            ingredients = new ArrayList<>();
        }

        public void changeTargetPortions(float targetPortions)
        {
            this.targetPortions = targetPortions;
            if (mealSuggestion.determineIngredients())
            {
                mealSuggestion.updateDisplayValid(binding);
            }
            else
            {
                mealSuggestion.updateDisplayInvalid(binding);
            }
        }

        /** Return chosen meal back to the selectable pool if necessary. */
        public void resetMealsPool()
        {
            if (chosenMeal != null)
            {
                selectableMeals.add(chosenMeal);
                chosenMeal = null;
            }
        }

        /** Chose meal from global lists. */
        public void choseMeal(final Random random)
        {
            chosenMeal = selectableMeals.remove(random.nextInt(selectableMeals.size()));
        }

        /** Determine ingredients based on chosen articles and target values. */
        public boolean determineIngredients()
        {
            totalWeight = 0F;
            totalSugar = 0F;
            float weight;
            float sugarPartialSum = 0F;
            float toppingsWeight;
            ingredients.clear();
            ArticleEntity article;
            int spoonCount;
            // Calculate and add spoons for topping articles based on topping percentage:
            for (int i = 0; i < toppingsCount; i++)
            {
                article = chosenToppingArticles.get(i);
                spoonCount = (int)Math.round(targetPortions * toppingPercentage / (article.getWeight() * toppingsCount));
                spoonCount = Math.max(spoonCount, 1);
                weight = spoonCount * (float)article.getWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * article.getSugarPercentage();
                sugarPartialSum += weight * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                ingredients.add(new IngredientEntity(article, spoonCount));
            }
            toppingsWeight = totalWeight;
            // Calculate and add spoons for non-last regular articles based on number of ingredients:
            for (int i = 0; i < regularArticlesCount - 1; i++)
            {
                article = chosenMeal.get(i);
                spoonCount = (int)Math.round((targetPortions - toppingsWeight) / (article.getWeight() * (regularArticlesCount + FILLER_INGREDIENT_RATIO)));
                spoonCount = Math.max(spoonCount, 1);
                weight = spoonCount * (float)article.getWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * article.getSugarPercentage();
                sugarPartialSum += weight * (article.getSugarPercentage() - fillerArticle.getSugarPercentage());
                ingredients.add(new IngredientEntity(article, spoonCount));
            }
            // Calculate and add spoons for last regular article based on sugar percentage:
            article = chosenMeal.get(regularArticlesCount - 1);
            spoonCount = (int)Math.round((targetSugar - targetPortions * fillerArticle.getSugarPercentage() - sugarPartialSum) / (article.getWeight() * (article.getSugarPercentage() - fillerArticle.getSugarPercentage())));
            if (spoonCount <= 0) return false;
            weight = spoonCount * (float)article.getWeight();
            totalSpoons += spoonCount;
            totalWeight += weight;
            totalSugar += weight * article.getSugarPercentage();
            ingredients.add(new IngredientEntity(article, spoonCount));
            // Calculate and add spoons for filler article based on total size:
            spoonCount = (int)Math.round((targetPortions - totalWeight) / fillerArticle.getWeight());
            if (spoonCount < 0) return false;
            if (spoonCount > 0)
            {
                weight = spoonCount * (float)fillerArticle.getWeight();
                totalSpoons += spoonCount;
                totalWeight += weight;
                totalSugar += weight * fillerArticle.getSugarPercentage();
                ingredients.add(new IngredientEntity(fillerArticle, spoonCount));
            }
            return true;
        }

        /** Adjust mix buttons and ingredients list for valid settings. */
        @SuppressLint("NotifyDataSetChanged")
        public void updateDisplayValid(final MainScreenBinding binding)
        {
            binding.setTotalSpoonCount(String.format(Locale.getDefault(), "%d spoons", mealSuggestion.totalSpoons));
            binding.setTotalWeight(String.format(Locale.getDefault(), "%.1f", mealSuggestion.totalWeight));
            binding.setTotalSugarPercentage(String.format(Locale.getDefault(), "%.1f", 100 * mealSuggestion.totalSugar / mealSuggestion.totalWeight));
            ingredientsAdapter.setIngredients(mealSuggestion.ingredients);
            binding.setIsChosenMuesliUsed(isChosenMealUsed = false);
            binding.setIsIngredientsListEmpty(false);
            binding.setIsInvalidSettings(false);
            ingredientsAdapter.notifyDataSetChanged();
        }

        /** Adjust mix buttons and ingredients list for invalid settings. */
        public void updateDisplayInvalid(final MainScreenBinding binding)
        {
            binding.setIsChosenMuesliUsed(isChosenMealUsed = true);
            binding.setIsIngredientsListEmpty(true);
            binding.setIsInvalidSettings(true);
        }
    }

    public void onRadioButtonClicked(@NonNull View view)
    {
        final int id = view.getId();
        if (id == R.id.mixMuesliButton)
        {
            userMode = UserMode.SUGGEST_MEAL;
        }
        else if (id == R.id.availabilityButton)
        {
            userMode = UserMode.AVAILABILITY;
            refreshData(true);
        }
        else if (id == R.id.editItemsButton)
        {
            userMode = UserMode.EDIT_ITEMS;
            refreshData(true);
        }
        binding.setUserMode(userMode);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        final Random random = new Random();
        binding = DataBindingUtil.setContentView(this, R.layout.main_screen);
        loadPreferences();
        mealSuggestion = null;

        // Setup activity result launcher for document handling:
        ActivityResultCallback<ActivityResult> createFileActivityCallback = result ->
        {
            try
            {
                final Intent resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final Uri targetUri = resultData.getData();
                    assert targetUri != null;
                    updateItemsJsonString();
                    writeTextFile(targetUri, itemsJsonString);
                }
            }
            catch (IOException e)
            {
                Log.e("createFileActivityCallback", "Export request failed");
                e.printStackTrace();
            }
        };
        createFileActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), createFileActivityCallback);
        ActivityResultCallback<ActivityResult> openFileActivityCallback = result ->
        {
            try
            {
                final Intent resultData = result.getData();
                if (result.getResultCode() == Activity.RESULT_OK && resultData != null)
                {
                    final Uri targetUri = resultData.getData();
                    assert targetUri != null;
                    itemsJsonString = readTextFile(targetUri);
                    mergeItemsJsonString();
                }
            }
            catch (IOException e)
            {
                Log.e("createFileActivityCallback", "Import request failed");
                e.printStackTrace();
            }
        };
        openFileActivityLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), openFileActivityCallback);

        // Setup adapters and layout managers:
        ingredientsAdapter = new IngredientsAdapter(this);
        binding.ingredients.setAdapter(ingredientsAdapter);
        binding.ingredients.setLayoutManager(new LinearLayoutManager(this));
        availableMealsAdapter = new MealsAdapter(this);
        binding.availableArticles.setAdapter(availableMealsAdapter);
        binding.availableArticles.setLayoutManager(new LinearLayoutManager(this));
        final ArrayAdapter<Type> typeSpinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, Type.values());
        binding.typeSpinner.setAdapter(typeSpinnerAdapter);

        // Load all articles and add them to the fitting state lists:
        if (loadArticles(allMeals))
        {
            availableMealsAdapter.setMeals(allMeals);
        }
        try
        {
            final Resources resources = this.getResources();
            itemsJsonString = readTextFile(new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(resources.getResourcePackageName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceTypeName(DEFAULT_ARTICLES_RES_ID)).appendPath(resources.getResourceEntryName(DEFAULT_ARTICLES_RES_ID)).build());
            mergeItemsJsonString();
        }
        catch (IOException e)
        {
            Log.e("onCreate", "Default muesli item data import failed");
            e.printStackTrace();
        }
        addMealsToFittingStateList(allMeals);
        if (!selectableMeals.isEmpty() && getLowestValue(selectableMeals, ArticleEntity::getSugarPercentage) <= getLowestValue(fillerArticles, ArticleEntity::getSugarPercentage)) throw new AssertionError("Sugar percentage of all filler articles has to be lower than that of regular articles");

        // Init layout variables:
        binding.setUserMode(userMode);
        binding.typeSpinner.setSelection(typeSpinnerAdapter.getPosition(Type.REGULAR));
        binding.setNewArticle(new ArticleEntity("", "", (Type)binding.typeSpinner.getSelectedItem(), 0F, 0F));
        refreshCountInfo();
        binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
        binding.setIsChosenMuesliUsed(isChosenMealUsed);
        binding.setIsIngredientsListEmpty(true);
        binding.setIsInvalidSettings(false);
        binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync

        // Create slider and button listeners:
        binding.sizeSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
        {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                sizeValue = progress;
                if (mealSuggestion != null)
                {
                    mealSuggestion.changeTargetPortions(sizeValue2SizeWeight(sizeValue));
                }
                binding.setSizeWeight(String.format(Locale.getDefault(), "%.0f", sizeValue2SizeWeight(sizeValue)));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                storePreferences();
            }
        });
        binding.typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id)
            {
                final Type selectedType = (Type)(adapterView.getItemAtPosition(position));
                final ArticleEntity newArticle = (ArticleEntity)binding.getNewArticle();
                newArticle.setType(selectedType);
                binding.setNewArticle(newArticle);  // Required to update weight field hint
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        binding.addButton.setOnClickListener((View view) ->
        {
            final ArticleEntity newArticle = (ArticleEntity)binding.getNewArticle();
            // Check for name duplicate:
            final boolean isDuplicate = allMeals.stream().anyMatch(article -> isNameTheSame(article, newArticle));
            // Check for empty name or brand:
            final String newName = newArticle.getName();
            final String newBrand = newArticle.getBrand();
            final boolean hasEmptyField = (newName == null || Objects.equals(newName, "") || newBrand == null || Objects.equals(newBrand, ""));
            // Add and sort in new article, update adapter and scroll to its position:
            if (!isDuplicate && !hasEmptyField)
            {
                allMeals.add(newArticle);
                addMealsToFittingStateList(Collections.singletonList(newArticle));
                allMeals.sort(Comparator.comparing(article -> (article.getBrand() + article.getName())));
                availableMealsAdapter.setMeals(allMeals);
                binding.availableArticles.smoothScrollToPosition(allMeals.indexOf(newArticle));
                binding.setNewArticle(new ArticleEntity("", "", newArticle.getType(), 0F, 0F));
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
            }
            else
            {
                Log.i("onCreate", "Cannot add empty or duplicate muesli name");
            }
        });
        binding.importButton.setOnClickListener((View view) -> openFileActivityLauncher.launch(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(INTENT_TYPE_JSON)));
        binding.exportButton.setOnClickListener((View view) -> createFileActivityLauncher.launch(new Intent(Intent.ACTION_CREATE_DOCUMENT).putExtra(Intent.EXTRA_TITLE, EXPORT_ITEMS_FILENAME + ".json").addCategory(Intent.CATEGORY_OPENABLE).setType(INTENT_TYPE_JSON)));
        binding.nameField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.brandField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.weightField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.percentageField.setOnFocusChangeListener(this::setEditTextFocus);
        binding.newButton.setOnClickListener((View view) ->
        {
            final float targetWeight = sizeValue2SizeWeight(sizeValue);
            mealSuggestion = new MealSuggestion(targetWeight);

            // Return used meals back to the selectable pool if necessary:
            if (selectableMeals.size() <= 0)
            {
                if (chosenMeal != null)
                {
                    selectableMeals.add(chosenMeal);
                    chosenMeal = null;
                }
                usedMeals.forEach((MealEntity meal) -> meal.setSelectionsLeft(meal.getMultiplier()));
                selectableMeals.addAll(usedMeals);
                usedMeals.clear();
            }

            // Check general conditions for valid suggestion:
            if (selectableMeals.size() <= 0)
            {
                mealSuggestion.updateDisplayInvalid(binding);
                binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
                mealSuggestion = null;
                Log.i("onCreate", "Not enough available meals for a valid mix");
                return;
            }

            // Chose and display random meal:
            mealSuggestion.resetMealsPool();
            mealSuggestion.choseMeal(random);
            mealSuggestion.determineIngredients();
            mealSuggestion.updateDisplayValid(binding);
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes sync
        });
        binding.useButton.setOnClickListener((View view) ->
        {
            if (chosenMeal == null)
            {
                Log.e("onCreate", "ChosenArticles is empty");
            }

            // Decrement selections left and move meal to fitting pool:
            chosenMeal.decrementSelectionsLeft();
            addMealsToFittingStateList(chosenMeal);
            chosenMeal = null;

            // Adjust mix buttons and ingredients list:
            binding.setIsChosenMuesliUsed(isChosenMealUsed = true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes
            mealSuggestion = null;

            // Store updated articles in memory:
            storeArticles(allMeals);
        });
        binding.clearButton.setOnClickListener((View view) ->
        {
            // Return chosen articles back to the selectable pool if necessary:
            if (chosenMeal != null)
            {
                selectableMeals.add(chosenMeal);
                chosenMeal = null;
            }

            // Adjust mix buttons and ingredients list:
            ingredientsAdapter.setIngredients(new ArrayList<>(0));
            binding.setIsChosenMuesliUsed(isChosenMealUsed = true);
            binding.setIsIngredientsListEmpty(true);
            refreshCountInfo();
            ingredientsAdapter.notifyDataSetChanged();
            binding.executePendingBindings();  // Espresso does not know how to wait for data binding's loop so we execute changes
            mealSuggestion = null;

            // Store updated articles in memory:
            storeArticles(allMeals);
        });
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        // Store updated articles and preferences in memory:
        storeArticles(allMeals);
        storePreferences();
    }
}
