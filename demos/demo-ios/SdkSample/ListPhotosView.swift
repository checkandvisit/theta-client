//
//  ListPhotosView.swift
//  SdkSample
//
import SwiftUI

class FileItem: Identifiable {
  var name: String
  var url: String
  var thumbnail: String
  init(name: String = "", url: String = "", thumbnail: String = "") {
    self.name = name
    self.url = url
    self.thumbnail = thumbnail
  }
}

struct ListPhotosView: View {
  @Environment(\.presentationMode) var presentation
  @State var files: [FileItem] = []

  var body: some View {
    List(files) { item in
      HStack {
        AsyncImage(url: URL(string: item.thumbnail)) { image in
          image.resizable()
        } placeholder: {
          ProgressView()
        }
        .frame(width: 80, height: 40)
        Text(item.name)
        NavigationLink(destination: PhotoSphereView(item)) {}
      }
    }
    .refreshable {
      Task {
        await listPhotos { newFiles in
          files = newFiles
        }
      }
    }
    .onAppear {
      Task {
        await listPhotos { newFiles in
          files = newFiles
        }
      }
    }
    .navigationBarBackButtonHidden(true)
    .navigationTitle(getTitle())
    .navigationBarItems(
      leading:
      HStack {
        Button(action: {
                 presentation.wrappedValue.dismiss()
               },
               label: {
                 Image("chevron")
                   .resizable()
                   .frame(width: 24, height: 24)
               })
      }
    )
    .listStyle(.plain)
  }

  func listPhotos(_ done: @escaping (_ files: [FileItem]) -> Void) async {
    do {
      try await theta.listPhotos { response in
        var newFiles: [FileItem] = []
        for item in response {
          newFiles.append(
            FileItem(
              name: item.name,
              url: item.fileUrl,
              thumbnail: item.thumbnailUrl
            ))
        }
        done(newFiles)
      }
    } catch {
      done([])
    }
  }

  func getTitle() -> String {
    if let thetaInfo = theta.lastInfo {
      thetaInfo.model + ":" + thetaInfo.serialNumber
    } else {
      "not connected"
    }
  }
}

struct ListPhotosView_Previews: PreviewProvider {
  static var previews: some View {
    ListPhotosView()
  }
}
