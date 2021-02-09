package com.fakeorreal.fakeorreal;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class ImageGenerator {

    public static String FIREBASE_COLLECTION_IMAGES = "images";
    public static String FIREBASE_DOCUMENT_IMAGES_CORRECT_GUESSES = "correct_guesses";
    public static String FIREBASE_DOCUMENT_IMAGES_WRONG_GUESSES = "wrong_guesses";

    private List<RequestBuilder<Drawable>> _realImages; // batch for real images
    private List<RequestBuilder<Drawable>> _fakeImages; // batch for fake images
    private List<String> _fakeIDs; // corresponding ids of _fakeImages
    private List<String> _realIDs; // corresponding ids of _realImages
    private Map<String, List<Integer>> _imagesInfo; // key: image id, value: [correct guesses, wrong guesses]
    private static ImageGenerator imageGenerator = null;  // this is a singelton
    private Context _context;
    private FirebaseFirestore _FireStore;


    private ImageGenerator(BufferedReader real_reader, BufferedReader fake_reader, Context context){
        _FireStore = ((FakeOrRealApp) context).getFireStoreInstance();
        _context = context;
        _realImages = new ArrayList<>();
        _fakeImages = new ArrayList<>();
        _realIDs = getIDList(real_reader, true);
        _fakeIDs = getIDList(fake_reader, false);
        setImagesInfo();
        getImagesInformation();


    }

    static ImageGenerator getInstance(BufferedReader real_reader, BufferedReader fake_reader, Context context){
        if (imageGenerator == null){
            imageGenerator = new ImageGenerator(real_reader, fake_reader, context);
        }
        return imageGenerator;
    }

    private void setImagesInfo(){
        _imagesInfo = new HashMap<>();
        for (String id : _fakeIDs){
            List<Integer> zeros = new ArrayList<>(Arrays.asList(0, 0));
            _imagesInfo.put(id, zeros);
        }
    }

    /**
     * @param real - true iff the batch should contains real images
     * @return - id of a random batch (csv file)
     */
    static public int getRandomIDBatch(boolean real){
        int chosen_batch;
        int idx;
        Random random = new Random();
        List<Integer> all_batches;
        if (real){
            all_batches = new ArrayList<>(Arrays.asList(
                    // list of all real batches..
                    R.raw.real_04k_to_05k,
                    R.raw.real_18k_to_19k,
                    R.raw.real_20k_to_21k,
                    R.raw.real_41k_to_42k,
                    R.raw.real_46k_to_47k
            ));
            idx = random.nextInt(all_batches.size());
            chosen_batch = all_batches.get(idx);
        }
        else {
            all_batches = new ArrayList<>(Arrays.asList(
                    // list of all fake batches..
                    R.raw.fake_06k_to_07k,
                    R.raw.fake_20k_to_21k,
                    R.raw.fake_36k_to_37k,
                    R.raw.fake_50k_to_51k,
                    R.raw.fake_93k_to_94k
            ));
            idx = random.nextInt(all_batches.size());
            chosen_batch = all_batches.get(idx);
        }
        return chosen_batch;
    }

    /**
     * Adding all the images into a List and return List of the corresponding ids
     * @param reader - the BufferReader of the chosen csv
     * @param isReal - true iff the reader contains real images
     * @return
     */
    private List<String> getIDList(BufferedReader reader, boolean isReal) {
        List<String> id_list = new ArrayList<>();
        try {
            String line; // Start reading the file, line by line
            while ((line = reader.readLine()) != null) {
                String id = line.split(",")[1];  // id at index 1
                id_list.add(id);
                if (isReal){
                    _realImages.add(Glide.with(_context).load(GameActivity.GOOGLE_API + id));
                }
                else {
                    _fakeImages.add(Glide.with(_context).load(GameActivity.GOOGLE_API + id));
                }

            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return id_list;
    }

    public List<String> get_fakeIDs() {
        return _fakeIDs;
    }

    public List<String> get_realIDs() {
        return _realIDs;
    }

    public List<RequestBuilder<Drawable>> get_realImages() {
        return _realImages;
    }

    public List<RequestBuilder<Drawable>> get_fakeImages() {
        return _fakeImages;
    }

    public int getFakeRandomIndex() {
        Random random = new Random();
        return random.nextInt(get_fakeIDs().size());
    }

    public int getRealRandomIndex() {
        Random random = new Random();
        return random.nextInt(get_realIDs().size());
    }

    /**
     * If image is in the fireStore - update it in _imageInfo.
     */
    void getImagesInformation(){
        _FireStore.collection(FIREBASE_COLLECTION_IMAGES).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()){
                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())){
                        String id = document.getId();
                        if (!_imagesInfo.containsKey(id)){
                            continue;  // We want to work only on images that in the current batch
                        }
                        // getting the information from the fireBase.
                        Map<String, Object> map = document.getData();
                        int correct_guesses_to_add = Integer.parseInt((String) Objects.requireNonNull(map.get(FIREBASE_DOCUMENT_IMAGES_CORRECT_GUESSES)));
                        int wrong_guesses_to_add = Integer.parseInt((String) Objects.requireNonNull(map.get(FIREBASE_DOCUMENT_IMAGES_WRONG_GUESSES)));

                        // it should be zeros, but better to be on the safe side
                        int current_correct_guesses = getNumberOfCorrectAnswers(id);
                        int current_wrong_guesses = getNumberOfWrongAnswers(id);

                        List<Integer> listToUpdate = new ArrayList<>(Arrays.asList(
                                current_correct_guesses + correct_guesses_to_add,
                                current_wrong_guesses + wrong_guesses_to_add
                        ));
                        // update the information
                        _imagesInfo.put(id, listToUpdate);
                    }
                }
            }
        });
    }

    public int getNumberOfCorrectAnswers(String id){
        return Objects.requireNonNull(_imagesInfo.get(id)).get(0);
    }

    public int getNumberOfWrongAnswers(String id){
        return Objects.requireNonNull(_imagesInfo.get(id)).get(1);
    }

    public void increaseCorrectAnswer(String id){
        int current_correct_answers = getNumberOfCorrectAnswers(id);
        Objects.requireNonNull(_imagesInfo.get(id)).set(0, current_correct_answers + 1);
    }

    public void increaseWrongAnswer(String id){
        int current_wrong_answers = getNumberOfWrongAnswers(id);
        Objects.requireNonNull(_imagesInfo.get(id)).set(1, current_wrong_answers + 1);
    }

    public RequestBuilder<Drawable> getImageById(String id) {
        return Glide.with(_context).load(GameActivity.GOOGLE_API + id);
    }

}
