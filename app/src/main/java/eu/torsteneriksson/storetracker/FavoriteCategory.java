package eu.torsteneriksson.storetracker;

/**
 * Created by torsten on 2015-11-21.
 */
public class FavoriteCategory {

    public static int getCategoryImage (int category) {
        switch (category){
            case 0:
                return R.drawable.ic_favorite;
            case 1:
                return R.drawable.ic_store;
            case 2:
                return R.drawable.ic_restaurant;
            case 3:
                return R.drawable.ic_cafe;
            case 4:
                return R.drawable.ic_nature_spot;
            case 5:
                return R.drawable.ic_home;
            case 6:
                return R.drawable.ic_friend;
            case 7:
                return R.drawable.ic_attraction;
            case 8:
                return R.drawable.ic_culture;
            default:
                return R.drawable.ic_favorite;
        }
    }
    public static int getCategoryText (int category) {
        switch (category){
            case 0:
                return R.string.other;
            case 1:
                return R.string.store;
            case 2:
                return R.string.restaurant;
            case 3:
                return R.string.coffee;
            case 4:
                return R.string.nature_spot;
            case 5:
                return R.string.home;
            case 6:
                return R.string.friend;
            case 7:
                return R.string.attraction;
            case 8:
                return R.string.culture;
            default:
                return R.string.other;
        }
    }

}

