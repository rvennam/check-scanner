gitbook build
mkdir build_gh_pages
cd build_gh_pages
git clone git@github.com:rvennam/check-scanner.git -b gh-pages
cd check-scanner
cp -a ../../_book/* .
git add .
git commit -m "local updates"
git push origin gh-pages
cd ../..
rm -rf ./build_gh_pages