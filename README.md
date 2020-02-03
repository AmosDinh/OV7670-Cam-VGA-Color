# OV7670-Cam-VGA-Color
This is currently in progress.
Using work of @ComputerNerd's ov7670-no-ram-arduino-uno and 
https://www.youtube.com/watch?v=6bfY9JXOppI (<- follow this tutorial for setup. If you wish to compile SimpleRead.java yourself, use "javac --release 8")

Please forgive me if my code is unorthodox somehow.
Use my modified version of the SimpleRead.class from the video and the .ino to the Arduino Uno.

Current fps is really low, I adjusted pclk, otherwise Images will be corrupted. Aimed at making a timelapse, not speed.
Currently saves an image in raw-like fomat(pattern like bayer rgb)
as bmp(.ppm) to the folder C:/out.         
                                           
                                        
Can be opened with photoshop or infraview.





