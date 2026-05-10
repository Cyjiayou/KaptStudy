package com.cy.compiler.activity.builder;

import javax.naming.Context;

public final class SecondActivityBuilder {

//    public static void start(Context context, String name, String age, String company, String title, String workPlace) {
//        Intent intent = new Intent(context, SecondActivity.class);
//        intent.putExtra("name", name);
//        intent.putExtra("age", age);
//        intent.putExtra("company", company);
//        intent.putExtra("title", title);
//        intent.putExtra("workPlace", workPlace);
//        ActivityBuilder.INSTANCE.startActivity(context, intent)
//    }
//
//
//    public static void startWithoutOptional(Context context, String name, String age) {
//        Intent intent = new Intent(context, SecondActivity.class);
//        intent.putExtra("name", name);
//        intent.putExtra("age", age);
//
//
//    }

//    public static void startWithOptional(String url) {
//        Intent intent = new Intent(context, SecondActivity.class);
//        intent.putExtra("name", name);
//        intent.putExtra("age", age);
//        intent.putExtra("url", url);
//    }


//    private String company;
//
//    private String workPlace;
//
//    private String title;
//
//
//    public SecondActivityBuilder setCompany(String company) {
//        this.company = company;
//        return this;
//    }
//
//    private void fillOptions(Intent intent) {
//        if (this.company != null) {
//            intent.putExtra("company", this.company);
//        }
//        if (this.title != null) {
//            intent.putExtra("company", this.title);
//        }
//        if (this.workPlace != null) {
//            intent.putExtra("company", this.workPlace);
//        }
//    }
//
//    public static void startWithOptionals() {
//        Intent intent = new Intent(context, SecondActivity.class);
//        intent.putExtra("name", name);
//        intent.putExtra("age", age);
//        fillOptions(intent);
//    }

//    public static void inject(Activity activity, Bundle bundle) {
//        if (activity instanceof SecondActivity) {
//            SecondActivity typedActivity = (SecondActivity) activity;
//            Bundle extras = bundle == null ? typedActivity.getIntent().getExtras(): bundle;
//            if (extras != null) {
//                Integer ageValue = BundleUtils.<Integer>get(extras, "age");
//                typedActivity.setAge(ageValue);
//            }
//        }
//    }

//    public static void saveState(Activity activity, Bundle outState) {
//        if (activity instanceof SecondActivity) {
//            SecondActivity typedActivity = (SecondActivity) activity;
//            Intent intent = new Intent();
//            intent.putExtra("age", typedActivity.getAge());
//
//            outState.putAll(intent.getExtras());
//        }
//    }
}
