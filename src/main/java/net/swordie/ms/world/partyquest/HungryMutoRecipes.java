package net.swordie.ms.world.partyquest;

public class HungryMutoRecipes {

    private int id;
    private int recipeItem = 0, recipeReq = 0, recipeCount = 0;
    private boolean recipeHidden = false;

    public HungryMutoRecipes(int id, int recipeItem, int recipeReq, int recipeCount) {
        this.id = id;
        this.recipeItem = recipeItem;
        this.recipeReq = recipeReq;
        this.recipeCount = recipeCount;
    }

    public int getRecipeItem() {
        return recipeItem;
    }

    public void setRecipeItem(int recipeItem) {
        this.recipeItem = recipeItem;
    }

    public int getRecipeReq() {
        return recipeReq;
    }

    public void setRecipeReq(int recipeReq) {
        this.recipeReq = recipeReq;
    }

    public int getRecipeCount() {
        return recipeCount;
    }

    public void setRecipeCount(int recipeCount) {
        this.recipeCount = recipeCount;
    }

    public boolean isRecipeHidden() {
        return recipeHidden;
    }

    public void setRecipeHidden(boolean recipeHidden) {
        this.recipeHidden = recipeHidden;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
