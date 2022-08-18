package ibm.tf.hangul;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String LABEL_FILE = "2350-common-hangul.txt";
    private static final String MODEL_FILE = "optimized_hangul_tensorflow.pb";

    private HangulClassifier classifier;
    private PaintView paintView;
    private Button alt1, alt2, alt3, alt4;
    private LinearLayout altLayout;
    private EditText resultText;
    private TextView translationText;
    private String[] currentTopLabels;
    private static final int pic_id = 123;
    Button camera_open_id;
    ImageView click_image_id;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        camera_open_id = (Button)findViewById(R.id.btnCam);
        click_image_id = (ImageView)findViewById(R.id.click_image);

        camera_open_id.setOnClickListener(new View.OnClickListener() {

                                              @Override
                                              public void onClick(View view) {
                                                  Intent camera_intent
                                                          = new Intent(MediaStore
                                                          .ACTION_IMAGE_CAPTURE);

                                                  // Start the activity with camera_intent,
                                                  // and request pic id
                                                  startActivityForResult(camera_intent, pic_id);
                                              }
                                          });

        paintView = (PaintView) findViewById(R.id.paintView);

        TextView drawHereText = (TextView) findViewById(R.id.drawHere);
        paintView.setDrawText(drawHereText);

        /*Button clearButton = (Button) findViewById(R.id.btnCam);
        clearButton.setOnClickListener(this); */

        Button classifyButton = (Button) findViewById(R.id.buttonClassify);
        classifyButton.setOnClickListener(this);

        Button backspaceButton = (Button) findViewById(R.id.buttonBackspace);
        backspaceButton.setOnClickListener(this);

        Button spaceButton = (Button) findViewById(R.id.buttonSpace);
        spaceButton.setOnClickListener(this);

        Button submitButton = (Button) findViewById(R.id.buttonSubmit);
        submitButton.setOnClickListener(this);

        altLayout = (LinearLayout) findViewById(R.id.altLayout);
        altLayout.setVisibility(View.INVISIBLE);

        alt1 = (Button) findViewById(R.id.alt1);
        alt1.setOnClickListener(this);
        alt2 = (Button) findViewById(R.id.alt2);
        alt2.setOnClickListener(this);
        alt3 = (Button) findViewById(R.id.alt3);
        alt3.setOnClickListener(this);
        alt4 = (Button) findViewById(R.id.alt4);
        alt4.setOnClickListener(this);

        translationText = (TextView) findViewById(R.id.translationText);
        resultText = (EditText) findViewById(R.id.editText);

        loadModel();
    }

    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data)
    {

        // Match the request 'pic id with requestCode
        if (requestCode == pic_id) {

            // BitMap is data structure of image file
            // which store the image in memory
            Bitmap photo = (Bitmap)data.getExtras()
                    .get("data");

            // Set the image in imageview for display
            click_image_id.setImageBitmap(photo);
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnCam:
                Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                startActivity(intent);
                break;
            case R.id.buttonClassify:
                classify();
                paintView.reset();
                paintView.invalidate();
                break;
            case R.id.buttonBackspace:
                backspace();
                altLayout.setVisibility(View.INVISIBLE);
                paintView.reset();
                paintView.invalidate();
                break;
            case R.id.buttonSpace:
                space();
                break;
            case R.id.buttonSubmit:
                altLayout.setVisibility(View.INVISIBLE);
                translate();
                break;
            case R.id.alt1:
            case R.id.alt2:
            case R.id.alt3:
            case R.id.alt4:
                useAltLabel(Integer.parseInt(view.getTag().toString()));
                break;
        }
    }


    private void backspace() {
        int len = resultText.getText().length();
        if (len > 0) {
            resultText.getText().delete(len - 1, len);
        }
    }


    private void space() {
        resultText.append(" ");
    }


    private void clear() {
        paintView.reset();
        paintView.invalidate();
        resultText.setText("");
        translationText.setText("");
        altLayout.setVisibility(View.INVISIBLE);
    }


    private void classify() {
        float pixels[] = paintView.getPixelData();
        currentTopLabels = classifier.classify(pixels);
        resultText.append(currentTopLabels[0]);
        altLayout.setVisibility(View.VISIBLE);
        alt1.setText(currentTopLabels[1]);
        alt2.setText(currentTopLabels[2]);
        alt3.setText(currentTopLabels[3]);
        alt4.setText(currentTopLabels[4]);
    }


    private void translate() {
        String text = resultText.getText().toString();
        if (text.isEmpty()) {
            return;
        }

        HashMap<String, String> postData = new HashMap<>();
        postData.put("text", text);
        postData.put("source", "ko");
        postData.put("target", "en");
        String apikey = getResources().getString(R.string.apikey);
        String url = getResources().getString(R.string.url);
        HangulTranslator translator = new HangulTranslator(postData, translationText, apikey, url);
        translator.execute();
    }


    private void useAltLabel(int index) {
        backspace();
        resultText.append(currentTopLabels[index]);
    }

    @Override
    protected void onResume() {
        paintView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        paintView.onPause();
        super.onPause();
    }


    private void loadModel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    classifier = HangulClassifier.create(getAssets(),
                            MODEL_FILE, LABEL_FILE, PaintView.FEED_DIMENSION,
                            "input", "keep_prob", "output");
                } catch (final Exception e) {
                    throw new RuntimeException("Error loading pre-trained model.", e);
                }
            }
        }).start();
    }
}
