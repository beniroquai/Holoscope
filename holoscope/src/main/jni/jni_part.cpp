#include "com_choochootrain_refocusing_activity_MainActivity.h"
#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/features2d/features2d.hpp>
#include <vector>
#include <stdlib.h>
#include <opencv2/opencv.hpp>
#include <opencv2/superres/superres.hpp>

using namespace std;
using namespace cv;

extern "C" {
JNIEXPORT void JNICALL  Java_com_choochootrain_refocusing_activity_MainActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba);

JNIEXPORT void JNICALL  Java_com_choochootrain_refocusing_activity_MainActivity_FindFeatures(JNIEnv*, jobject, jlong addrGray, jlong addrRgba)
{ /*
    Mat& mGr  = *(Mat*)addrGray;
    Mat& mRgb = *(Mat*)addrRgba;
    vector<KeyPoint> v;

    Ptr<FeatureDetector> detector = FastFeatureDetector::create(50);
    detector->detect(mGr, v);
    for( unsigned int i = 0; i < v.size(); i++ )
    {
        const KeyPoint& kp = v[i];
        circle(mRgb, Point(kp.pt.x, kp.pt.y), 10, Scalar(255,0,0,255));
    }
}
*/


using namespace cv;
	using namespace cv::superres;

        // ?????????????
	Ptr<SuperResolution> superResolution = createSuperResolution_BTVL1();
        // ?????????????????
	superResolution->setInput(createFrameSource_Video("douga.mp4"));
        // ????????????????????????????????????
	superResolution->set("scale", 2);
	superResolution->set("temporalAreaRadius", 1);
	superResolution->set("iterations", 2);

	while (waitKey(1) == -1)
	{
                // ???????????????????
		Mat frame;
		superResolution->nextFrame(frame);
		imshow("Super Resolution", frame);
	}

}
}
