package com.stackd.stackd.activities;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.stackd.stackd.R;
import com.stackd.stackd.db.DataManager;
import com.stackd.stackd.db.entities.Resume;
import com.stackd.stackd.db.entities.Tag;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditActivity extends AppCompatActivity {
    public static final String IMAGE_URI_KEY = "imageUri";
    public static final String IMAGE_R_KEY = "rKey";
    private LinearLayout tagListLayout;
    private Map<Tag, CheckBox> checkBoxes = new HashMap<>();
    private Resume resume;
    // Dummy Values
    private final long cId = 1;
    private final long rId = 21;
    private DataManager dataManager;
    private AlertDialog alertBox = null;
    private RelativeLayout drawLayout;
    private List<Tag> resumeTags;

    private Uri imgUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        setUpAlertBox(this);

        dataManager = DataManager.getDataManager(cId, rId, getApplicationContext());
        // Fields needed from other screens
        Bundle extras = getIntent().getExtras();
        String strUri = extras.getString(IMAGE_URI_KEY);
        // set resume view to the picture of the current resume
        ImageView resumeView = (ImageView)findViewById(R.id.current_resume);
        if(strUri != null) {
            Log.d("URI", strUri);
            imgUri = Uri.parse(strUri);
            resumeView.setImageURI(imgUri);
        }
        long resumeId = extras.getLong(StackActivity.RESUME_ID_KEY);
        if(resumeId == StackActivity.RESUME_ID_NEW) {
            // Initialize the new resume
            resume = new Resume.Builder()
                    .id(DataManager.getNextResumeId())
                    .rid(dataManager.getRecruiter().getRecId())
                    .collectionDate(new SimpleDateFormat("DD-MM-yyyy").format(new Date()))
                    .candidateName("Candidate 1").build();
            resumeTags = new ArrayList<>();
        }
        else {
            // make a resume immutable, since it has already been reviewed
            // make adjustments to UI design
            Button btnDone = (Button) findViewById(R.id.submit_resume);
            btnDone.setEnabled(false);
            btnDone.setBackgroundColor(getResources().getColor(R.color.colorGrey));
            FloatingActionButton hlButton = (FloatingActionButton) findViewById(R.id.highlightButton);
            hlButton.setEnabled(false);
            hlButton.setBackgroundColor(getResources().getColor(R.color.colorGrey));
            EditText editText = ((EditText)findViewById(R.id.comment_field));
            editText.setTextColor(getResources().getColor(R.color.colorPrimary));
            editText.setEnabled(false);
            // load an existing resume
            for(Resume r : dataManager.getResumes())
                if(r.getId() == resumeId) {
                    resume = r;
                    editText.setText(resume.getRecruiterComments());
                    if(resume.getTagList() != null)
                        resumeTags = resume.getTagList();
                    else
                        resumeTags = new ArrayList<>();
                    break;
                }
        }
        assert(resume != null);
        // layout for highlighting
        drawLayout = (RelativeLayout) this.findViewById(R.id.drawLayout);
        drawLayout.setVisibility(RelativeLayout.GONE);

        // Add checkboxes dynamically
        tagListLayout = (LinearLayout) findViewById(R.id.tagListLayout);

        // The company's tags
        final List<Tag> tagList = dataManager.getCompanyTags();
        for (final Tag tag: tagList) {
            final CheckBox cb = new CheckBox(this);
            cb.setId(tagList.indexOf(tag));
            cb.setText(tag.getName());
            cb.setTextColor(Color.WHITE);
            cb.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    resumeTags.add(tag);
                }
            });
            if(resumeId != -1) {
                // make checkbox inactive and checked
                cb.setEnabled(false);
                if(resume.getTagList().contains(tag))
                    cb.setChecked(true);
            }
            tagListLayout.addView(cb);
        }
    }

    /*
     * Find the URI from the given resID
     */
    public static Uri resIdToUri(Context context, int resId) {
        return Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                context.getResources().getResourcePackageName(resId) + '/' +
                context.getResources().getResourceTypeName(resId) + '/' +
                context.getResources().getResourceEntryName(resId) );

    }

    public void onHighlightBtnClick(View v) {
        if (drawLayout.getVisibility() == RelativeLayout.GONE) {
            drawLayout.setVisibility(RelativeLayout.VISIBLE);
        } else {
            drawLayout.setVisibility(RelativeLayout.GONE);
        }
    }

    public void onDoneBtnClick(View v) {
        // add comments and selected tags to the resume
        final EditText commentField = (EditText) findViewById(R.id.comment_field);
        String comments = commentField.getText().toString();
        resume.setRecruiterComments(commentField.getText().toString());
        resume.setTagList(resumeTags);

        // upload the resume to the S3 bucket
        File newResumeImg = new File(imgUri.getPath());
        dataManager.uploadFile(resume.getCandidateName(), newResumeImg);
        alertBox.show();
    }

    public void setUpAlertBox(Context context) {

        // The button icons
        //ImageView no = new ImageView(this);
        //no.setImageResource(R.drawable.splattered);

        // setup the alert builder
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Candidate Status");
        //alertBuilder.setView(no);
        // add a list
        String[] ratings = {"Yes", "No", "Maybe"};

        alertBuilder.setItems(ratings, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: resume.setRating(2);// yes
                    case 1: resume.setRating(0);// no
                    case 2: resume.setRating(1);// maybe

                }
                // Insert the resume into the database
                assert(resume != null);
                dataManager.insertResume(resume);
                // Review it and add a rating
                dataManager.addReview(resume.getId(), resume.getCollectionDate(), resume.getRating());

                // Go back to stack view
                Intent i = new Intent(EditActivity.this, StackActivity.class);
                startActivity(i);
            }
        });
        alertBox = alertBuilder.create();
    }

}

