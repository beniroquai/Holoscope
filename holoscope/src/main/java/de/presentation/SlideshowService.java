/***
  Copyright (c) 2013-2014 CommonsWare, LLC
  
  Licensed under the Apache License, Version 2.0 (the "License"); you may
  not use this file except in compliance with the License. You may obtain
  a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package de.presentation;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.commonsware.cwac.preso.PresentationService;

import de.holoscope.R;

public class SlideshowService extends PresentationService implements
    Runnable {
  private static final int[] SLIDES= { R.drawable.img0,
      R.drawable.img1, R.drawable.img2, R.drawable.img3};
  private ImageView iv=null;
  private int iteration=0;
  private Handler handler=null;

  @Override
  public void onCreate() {
    handler=new Handler(Looper.getMainLooper());
    super.onCreate();
  }

  @Override
  protected int getThemeId() {
    return(R.style.AppTheme);
  }

  @Override
  protected View buildPresoView(Context ctxt, LayoutInflater inflater) {
    iv=new ImageView(ctxt);
    run();

    return(iv);
  }

  @Override
  public void run() {
    iv.setImageResource(SLIDES[iteration % SLIDES.length]);
    iteration+=1;

    handler.postDelayed(this, 5000);
  }

  @Override
  public void onDestroy() {
    handler.removeCallbacks(this);

    super.onDestroy();
  }
}
