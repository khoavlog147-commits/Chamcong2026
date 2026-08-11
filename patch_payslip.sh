#!/bin/bash

# We will use awk to replace the block for UI from line 627 to 708
# And the block for PNG from line 1163 to 1241

awk '
BEGIN { replacingUI=0; replacingPNG=0 }
NR==627 { replacingUI=1; system("cat update_payslip_additions.kt"); next }
replacingUI==1 && NR<=708 { next }
replacingUI==1 && NR>708 { replacingUI=0 }

NR==1163 { replacingPNG=1; system("cat update_payslip_png_additions.kt"); next }
replacingPNG==1 && NR<=1241 { next }
replacingPNG==1 && NR>1241 { replacingPNG=0 }

{ print $0 }
' app/src/main/java/com/example/ui/screens/PayslipScreen.kt > temp.kt

mv temp.kt app/src/main/java/com/example/ui/screens/PayslipScreen.kt
