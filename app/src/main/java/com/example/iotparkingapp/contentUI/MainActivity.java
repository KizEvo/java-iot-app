package com.example.iotparkingapp.contentUI;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.iotparkingapp.R;
import com.example.iotparkingapp.contentUI.car.CarInformation;
import com.example.iotparkingapp.contentUI.user.Gender;
import com.example.iotparkingapp.contentUI.user.UserInformation;

import com.example.iotparkingapp.loginUI.registerUserInformation;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class MainActivity extends AppCompatActivity {
    ViewPager2 viewPager2;
    private boolean isCarReferenceLoaded = false;
    private boolean isInitialDataLoaded = false;
    TabLayout tabLayout;
    FragmentAdapter fragmentAdapter;
    static CarInformation carInfor = new CarInformation();
    static UserInformation userInfor = new UserInformation();
    Map<String, String> slotsMap = new  HashMap<String, String>();
    FirebaseAuth fAuth;
    FirebaseFirestore fStore;
    FirebaseFirestore carStatus;
    String UID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        carStatus = FirebaseFirestore.getInstance();
        UID = fAuth.getCurrentUser().getUid();
        Log.d("Firestore", "UID: " + UID);


        setupViewPager();
        setupFirestoreListener();

    }


    public AccountFragment getAccountFragment() {
        return fragmentAdapter.getAccountFragment();
    }

    public CarInformation getCarInfor() {
        return carInfor;
    }

    public UserInformation getUserInfor() {
        return userInfor;
    }

    private void updateCarStatus(DocumentSnapshot value) {
        Map<String, List<String>> data = getMapWithDefault(value, "floor", createDefaultMap());;
        for (int i = 0; i < data.get("f1").size(); i++) {
            slotsMap.put("A" + (i + 1), data.get("f1").get(i));
        }

        for (int i = 0; i < data.get("f2").size(); i++) {
            slotsMap.put("B" + (i + 1), data.get("f2").get(i));
        }
    }
    private void updateUserInfo(DocumentSnapshot snapshot) {
        userInfor.setName(getFieldWithDefault(snapshot, "name", "Unknown"));
        userInfor.setSex(getGenderWithDefault(snapshot, "gender", Gender.Male));
        userInfor.setDate_of_birth(getFieldWithDefault(snapshot, "dob", "Unknown"));
        userInfor.setAddress(getFieldWithDefault(snapshot, "address", "Unknown"));
        userInfor.setPhone_number(getFieldWithDefault(snapshot, "phoneNum", "Unknown"));
    }

    private void updateCarInfo(DocumentSnapshot snapshot) {
        carInfor.setBeginDate(getFieldWithDefault(snapshot, "carBeginDate", ""));
        carInfor.setEndDate(getFieldWithDefault(snapshot, "carEndDate", ""));
        carInfor.setBrand(getFieldWithDefault(snapshot, "carBranch", "Unknown"));
        carInfor.setLicensePlate(getFieldWithDefault(snapshot, "carLicensePlate", "Unknown"));
    }


    private Map<String, List<String>> getMapWithDefault(DocumentSnapshot snapshot, String field ,Map<String, List<String>> defaultValue) {
        return snapshot.contains(field) ? (Map<String, List<String>>)snapshot.get(field) : defaultValue;
    }

    private Map<String, List<String>> createDefaultMap() {
        Map<String, List<String>> map = new HashMap<String, List<String>>();
        List<String> emptyList = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            emptyList.add("empty");
        }
        map.put("f1", emptyList);
        map.put("f2", emptyList);
        return map;
    }

    private String getFieldWithDefault(DocumentSnapshot snapshot, String field, String defaultValue) {
        return snapshot.contains(field) ? snapshot.getString(field) : defaultValue;
    }


    private Gender getGenderWithDefault(DocumentSnapshot snapshot, String field, Gender defaultValue) {
        try {
            return snapshot.contains(field) ?
                    Gender.valueOf(snapshot.getString(field)) : defaultValue;
        } catch (IllegalArgumentException e) {
            Log.w("Firestore", "Invalid gender value", e);
            return defaultValue;
        }
    }

    private void navigateToRegistration() {
        Toast.makeText(MainActivity.this, "Registration personal information required", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(MainActivity.this, registerUserInformation.class);
        startActivity(intent);
        finish();
    }

    private void setupFirestoreListener() {
        DocumentReference documentReference = fStore.collection("Users").document(UID);

        getCarReferenceNearestDay(carReference -> {
                    carReference.addSnapshotListener(this, (value, error) -> {
                        if (error != null) {
                            Log.e("Firestore", "Listen failed", error);
                            Toast.makeText(MainActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (value == null || !value.exists()) {
                            Log.w("Firestore", "Document does not exist");
                            navigateToRegistration();
                            return;
                        }

                        try {
                            updateCarStatus(value);
                            StatusFragment statusFragment = fragmentAdapter.getStatusFragment();
                            if (statusFragment != null) {
                                statusFragment.updateCarStatus(slotsMap);
                            }
                        } catch (IllegalArgumentException e) {
                            Log.e("Firestore", "Invalid data format", e);
                            Toast.makeText(MainActivity.this, "Invalid data format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("Firestore", "Unexpected error", e);
                            Toast.makeText(MainActivity.this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
        );



        documentReference.addSnapshotListener(this, (value, error) -> {
            if (error != null) {
                Log.e("Firestore", "Listen failed", error);
                Toast.makeText(MainActivity.this, "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value == null || !value.exists()) {
                Log.w("Firestore", "Document does not exist");
                navigateToRegistration();
                return;
            }

            try {
                updateUserInfo(value);
                updateCarInfo(value);
                isInitialDataLoaded = true;

                // Update the AccountFragment if it exists
                AccountFragment accountFragment = fragmentAdapter.getAccountFragment();
                if (accountFragment != null) {
                    accountFragment.updateUserInfo(userInfor);
                    accountFragment.updateCarInfo(carInfor);
                }

                // Update the HomeFragment if it exists
                HomeFragment homeFragment = fragmentAdapter.getHomeFragment();
                if (homeFragment != null) {
                    homeFragment.updateNameInfo(userInfor.getName());
                }

                Log.d("Firestore", "Data loaded successfully");

            } catch (IllegalArgumentException e) {
                Log.e("Firestore", "Invalid data format", e);
                Toast.makeText(MainActivity.this, "Invalid data format: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToRegistration();
            } catch (Exception e) {
                Log.e("Firestore", "Unexpected error", e);
                Toast.makeText(MainActivity.this, "An error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                navigateToRegistration();
            }
        });
    }

    private void getCarReferenceNearestDay(Consumer<DocumentReference> onComplete) {
        FirebaseFirestore carTemp = FirebaseFirestore.getInstance();
        checkCarCollection(carTemp, 0, onComplete);
    }

    private void checkCarCollection(FirebaseFirestore db, int daysBack, Consumer<DocumentReference> onComplete) {
        if (daysBack >= 100) {
            onComplete.accept(db.collection("sensorDataCollectionWedOct162024")
                    .document("collection-metadata"));
            return;
        }

        String carCollection = getCarCollectionDay(LocalDate.now().minusDays(daysBack));
        DocumentReference docRef = db.collection(carCollection).document("collection-metadata");

        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                Log.d("Firestore", "checkCarCollection is: " + carCollection);
                Toast.makeText(MainActivity.this, "Car status updated", Toast.LENGTH_SHORT).show();
                onComplete.accept(docRef);
            } else {
                Log.d("Firestore", "checkCarCollection is not: " + carCollection);
                checkCarCollection(db, daysBack + 1, onComplete);
            }
        });
    }


    private String getCarCollectionDay(LocalDate date) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.ENGLISH);
        String FormatedDate = date.format(formatter).replace(" ", "");
        return "sensorDataCollection" + FormatedDate;
    }


    // Add this method to check if initial data is loaded
    public boolean isInitialDataLoaded() {
        return isInitialDataLoaded;
    }

    private void setupViewPager() {
        viewPager2 = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        fragmentAdapter = new FragmentAdapter(this);
        viewPager2.setAdapter(fragmentAdapter);
        viewPager2.setOffscreenPageLimit(3);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Home");
                    tab.setIcon(R.drawable.baseline_home_24);
                    break;
                case 1:
                    tab.setText("Status");
                    tab.setIcon(R.drawable.baseline_local_parking_24);
                    break;
                case 2:
                    tab.setText("Account");
                    tab.setIcon(R.drawable.baseline_account_circle_24);
                    break;
            }
        }).attach();
    }




}
