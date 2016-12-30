package uz.msu.mazash;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import us.msu.mazash.R;
import android.app.Activity;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnTouchListener {
    private static final String SITE = "https://medsa.uz/test.php/";

    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isSending = false;

    private ImageView progress = null;
    private TextView progressTv = null;
    private TextView result = null;
    Animation anim = null;
    Animation[] instr_anim = new Animation[10];
    ImageView[] instr = new ImageView[10];

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        RelativeLayout rl =(RelativeLayout)this.findViewById(R.id.rl);
        rl.setOnTouchListener(this);
        progress = (ImageView)this.findViewById(R.id.progress);
        progressTv = (TextView)this.findViewById(R.id.progressTv);
        result = (TextView)this.findViewById(R.id.Result);
        anim = AnimationUtils.loadAnimation(this, R.anim.progress);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
        		RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    	AppLog.logString(bufferSize+" bufferSize");

    	

    	Handler r = new Handler();
    	
    	instr[0] = (ImageView)this.findViewById(R.id.Instrument0);
    	instr[1] = (ImageView)this.findViewById(R.id.Instrument1);
    	instr[2] = (ImageView)this.findViewById(R.id.Instrument2);
    	instr[3] = (ImageView)this.findViewById(R.id.Instrument3);
    	instr[5] = (ImageView)this.findViewById(R.id.Instrument5);
    	instr[8] = (ImageView)this.findViewById(R.id.back1);
    	instr[9] = (ImageView)this.findViewById(R.id.back2);

    	instr_anim[0] = AnimationUtils.loadAnimation(this, R.anim.instrument0);
    	instr_anim[1] = AnimationUtils.loadAnimation(this, R.anim.instrument1);
    	instr_anim[2] = AnimationUtils.loadAnimation(this, R.anim.instrument2);
    	instr_anim[3] = AnimationUtils.loadAnimation(this, R.anim.instrument3);
    	instr_anim[5] = AnimationUtils.loadAnimation(this, R.anim.instrument5);
    	instr_anim[8] = AnimationUtils.loadAnimation(this, R.anim.back1);
    	instr_anim[9] = AnimationUtils.loadAnimation(this, R.anim.back2);

    	instr[0].startAnimation(instr_anim[0]);
    	r.postDelayed(new Runnable() {
			@Override
			public void run() {
				instr[1].startAnimation(instr_anim[1]);
				instr[1].setVisibility(ImageView.INVISIBLE);
			}
		}, 2600);
    	r.postDelayed(new Runnable() {
			@Override
			public void run() {
				instr[2].startAnimation(instr_anim[2]);
				instr[2].setVisibility(ImageView.INVISIBLE);
			}
		}, 2600);
    	r.postDelayed(new Runnable() {
			@Override
			public void run() {
				instr[3].startAnimation(instr_anim[3]);
				instr[3].setVisibility(ImageView.INVISIBLE);
			}
		}, 350);
    	r.postDelayed(new Runnable() {
			@Override
			public void run() {
				instr[5].startAnimation(instr_anim[5]);
				instr[5].setVisibility(ImageView.INVISIBLE);
			}
		}, 2500);
    	
    }

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        AppLog.logString(file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording(){
    	instr[8].setVisibility(ImageView.VISIBLE);
    	instr[8].startAnimation(instr_anim[8]);
    	instr[9].startAnimation(instr_anim[9]);
    	instr[9].setVisibility(ImageView.VISIBLE);
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        int i = recorder.getState();
        if(i==1)
            recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile(){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;

        if(null != os){
            while(isRecording){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording(){
    	instr[8].setVisibility(ImageView.INVISIBLE);
    	instr[9].setVisibility(ImageView.INVISIBLE);
    	instr[8].clearAnimation();
    	instr[9].clearAnimation();
        if(null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if(i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
        final String filename = getFilename();
        copyWaveFile(getTempFilename(),filename);
        deleteTempFile();
        PostRequest post = new PostRequest();
        post.execute(filename);
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());

        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }
    class PostRequest extends AsyncTask<String, Integer, Void> {
        @Override
        protected void onPreExecute() {
          isSending = true;
          progress.setVisibility(ImageView.VISIBLE);
          progressTv.setVisibility(ImageView.VISIBLE);
          progress.startAnimation(anim);
          
          long mills = 1000L;
          Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
          vibrator.vibrate(mills);
          super.onPreExecute();
        }
        @Override
        protected Void doInBackground(String... filenames) {
        	for (String filename : filenames) {
                HttpRequest request = HttpRequest.post(SITE);
                request.part("uploadfile", "temp.wav", new File(filename));
            	AppLog.logString("starting");
                if(request.code() == 200) {
                	AppLog.logString("200");
					JSONObject json = null;
                	try {
						json = new JSONObject(request.body());
					} catch (Exception e) {
						e.printStackTrace();
						oops();
					}
                	try {
						final String s = json.getJSONArray("music").getJSONObject(0).getString("name");
						AppLog.logString(s);
	                	runOnUiThread(new Runnable() {
							@Override
							public void run() {
								result.setText(s);
							}
						});
					} catch (JSONException e) {
						AppLog.logString(e.getMessage());
						oops();
					}
                }else{
                	AppLog.logString(request.code()+"");
                	runOnUiThread(new Runnable() {
						@Override
						public void run() {
							Toast t = Toast.makeText(getApplicationContext(), "Service Temporarily Unavailable.\nPlease try later.", Toast.LENGTH_LONG);
							t.show();
						}
					});
                }
              }
          return null;
        }
        @Override
        protected void onPostExecute(Void result) {
          isSending = false;
          progressTv.setVisibility(ImageView.INVISIBLE);
          progress.clearAnimation();
          progress.setVisibility(ImageView.INVISIBLE);
          
          long mills = 1000L;
          Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
          vibrator.vibrate(mills);
          super.onPostExecute(result);
        }
    }

	  @Override
	  public boolean onTouch(View v, MotionEvent event) {
		    if(isSending)
		    	return true;
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	                AppLog.logString("ACTION_DOWN");
	                if(!isRecording)
	                	startRecording();
	                break;
	            case MotionEvent.ACTION_UP:
	                AppLog.logString("ACTION_UP");
	                if(isRecording)
	                	stopRecording();
	                v.performClick();
	                break;
	            case MotionEvent.ACTION_CANCEL:
	                AppLog.logString("ACTION_CANCEL");
	                if(isRecording)
	                	stopRecording();
	                break;
	        }
	        return true;
	  }
	  
	  public void oops(){
      	runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast t = Toast.makeText(getApplicationContext(), "Oops...\nSomething wrong...\nPlease try later.", Toast.LENGTH_LONG);
				t.show();
			}
		});
	  }
}