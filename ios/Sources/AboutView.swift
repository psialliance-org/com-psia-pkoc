import SwiftUI

struct AboutView : View

{
    var body: some View
    {
        VStack
        {
            HStack
            {
                Spacer().padding(1)

                Image(uiImage: UIImage(named: ProductImages.PSIA_Logo_Typographic)!)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(12)

                Image(uiImage: UIImage(named: ProductImages.PSIA_Logo)!)
                    .resizable()
                    .aspectRatio(contentMode: .fit)
                    .padding(12)
                
                Spacer().padding(1)
            }
                        
            Text("This application was developed in conjunction with the Physical Security Interoperability Alliance.")
                .padding(12)
            
            Divider()
            
            Text("PKOC (Public Key Open Credential) is an open standard that uses an asymmetric encryption key as an access control credential.")
                .padding(12)
            
            Divider()
            
            Text("This product includes software developed by the \"Marcin Krzyzanowski\" (http://krzyzanowskim.com/).'")
                .padding(12)
            
            Spacer()

            Text("Version: BLE-3.0, NFC-1.63")
        }
    }
}
