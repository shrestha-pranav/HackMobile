package org.tensorflow.demo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.snpeflow.demo.R;

public class ListActivity extends AppCompatActivity {
    ArrayList<RecipeBase.Recipe> recipeList = null;
    private static ArrayList<String> ingredients = new ArrayList<String>();
    public static void setIngredients(ArrayList<String> ingredients)
    {
        synchronized (ListActivity.ingredients)
        {
            ListActivity.ingredients = ingredients;
        }
    }



    public void setRecipeList(ArrayList<RecipeBase.Recipe> recipeList) {
        this.recipeList = recipeList;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        final ListView listview = (ListView) findViewById(R.id.list_view);

        if (recipeList == null) {
            RecipeBase tmp = new RecipeBase();
            recipeList = tmp.recipeList;
        }

        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0, len = recipeList.size(); i < len; ++i) {
            list.add(recipeList.get(i).recipeName);
        }

        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                String url = recipeList.get((int) id).recipeUrl;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
    }
}

class StableArrayAdapter extends ArrayAdapter<String> {
    HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

    public StableArrayAdapter(Context context, int textViewResourceId,
                              List<String> objects) {
        super(context, textViewResourceId, objects);
        for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i), i);
        }
    }

    public long getItemId(int position) {
        String item = getItem(position);
        return mIdMap.get(item);
    }

    public boolean hasStableIds() {
        return true;
    }
}
