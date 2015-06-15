{\rtf1\ansi\ansicpg1252\cocoartf1347\cocoasubrtf570
{\fonttbl\f0\fmodern\fcharset0 Courier;}
{\colortbl;\red255\green255\blue255;\red0\green0\blue0;}
\paperw11900\paperh16840\margl1440\margr1440\vieww10800\viewh8400\viewkind0
\deftab720
\pard\pardeftab720

\f0\fs24 \cf2 \expnd0\expndtw0\kerning0
\outl0\strokewidth0 \strokec2 public void Process(Image<Gray, byte> frame)\
    \{\
        SetRegionOfInterest(frame);\
\
        var resizedFrame = ResizeFrame(frame);\
\
        InputFrames.Add(resizedFrame);\
        if(InputFrames.Count > 5)\
        \{\
            InputFrames.RemoveAt(0);\
            PerformSuperResolution();\
        \}\
    \}\
\
    public void PerformSuperResolution()\
    \{\
        // WindowSize = 5\
        var referenceFrame = InputFrames[WindowSize-1].Convert<Gray, byte>();\
        var featuresToTrack = referenceFrame.GoodFeaturesToTrack(100, 0.1, 5, 10);\
\
        referenceFrame.FindCornerSubPix(featuresToTrack, new Size(WindowSize,WindowSize), new Size(-1,-1), new MCvTermCriteria(20, 0.03d));\
\
        var resultFrame = InputFrames[WindowSize-1].Convert<Gray, double>();\
        for(var frameCounter = 0; frameCounter < WindowSize-1; frameCounter++)\
        \{\
            // Get shift between frames\
            var shiftResult = GetShiftResult(InputFrames[frameCounter],referenceFrame, featuresToTrack);\
\
            // Warp to correct shift\
            var warpMatrix = new Matrix<double>(new[,] \{ \{1, 0, -shiftResult.ShiftX\}, \{0, 1, -shiftResult.ShiftY\}, \{0, 0, 1\}\});\
\
            var warpedFrame = InputFrames[frameCounter].WarpPerspective(warpMatrix, \
                INTER.CV_INTER_NN, \
                WARP.CV_WARP_DEFAULT, \
                new Gray(0)); \
\
            resultFrame.RunningAvg(warpedFrame.Convert<Gray,double>(), 1, resultFrame.Convert<Gray, byte>());\
        \}\
        SuperResolutionFrame = resultFrame.Convert<Gray, byte>();\
    \}\
\
    public ShiftResult GetShiftResult(Image<Gray, byte> inputFrame, Image<Gray, byte> referenceFrame, PointF[][] ActualFeature)\
    \{\
        var result = new ShiftResult();\
\
        PointF[] NextFeature;\
        Byte[] Status;\
        float[] TrackError;\
\
        // optical flow\
        OpticalFlow.PyrLK(referenceFrame, inputFrame, ActualFeature[0], \
            new Size(WindowSize, WindowSize), 5, new MCvTermCriteria(20, 0.1d), \
            out NextFeature, out Status, out TrackError);\
\
        //get displacements\
        float[] XdisplacementVectors = new float[NextFeature.Length];\
        float[] YdisplacementVectors = new float[NextFeature.Length];\
        for(int i = 0; i < NextFeature.Length; i++)\
        \{\
            XdisplacementVectors[i] = NextFeature[i].X - ActualFeature[0][i].X;\
            YdisplacementVectors[i] = NextFeature[i].Y - ActualFeature[0][i].Y;\
        \}\
\
        // gets average of displacements (disregards outliers)\
        result.ShiftX = getAVG(XdisplacementVectors);\
        result.ShiftY = getAVG(YdisplacementVectors);\
\
        return result;\
    \}\
\
\
\
\
\
\
for(int i  = 0; i < image_array.Count; i++)\
\{\
    Res.Process(image_array[i]);\
\}\
\
}