# GregTech Modern Documentation

This documentation project is built using [MkDocs](https://www.mkdocs.org/#).  
For an automatically updating live preview in your browser, run `mkdocs serve`

## Contributing

When contributing to this project, please refer to the [Styleguide](./CONTRIBUTING.md).  
This helps us in keeping the documentation consistent.
You will also find examples of when to use specific features of `mkdoc` and `mkdocs-material`.

## Quickstart Guide
If you want to contribute to the docs without setting up a dedicated code environment, you can go to the `<> Code` button on the main page of the repository.

![image](https://github.com/user-attachments/assets/27458f12-15af-475e-9e79-f45b890d4707)

Then, click `Codespaces` and create a new codespace on `1.20.1`

![image](https://github.com/user-attachments/assets/42b23f92-5277-4825-8a61-a44855f4e33c)

From there, you'll be taken to a VSCode instance in your browser, where you can edit any of the docs files in `/content`.  
If you want to preview your changes, just run `mkdocs serve` and click the link it gives you.

Once you're happy, commit these changes and make a pull request for us to review using the `Source Control` tab on the left. This will automatically create a fork of our repository for you.

If you come back to work on the docs, you can use a codespace again. You might need to `pull` to bring your codespace up to date, which you can do by pressing this button in the `Source Control` tab.

![image](https://github.com/user-attachments/assets/7d1246d2-f091-4452-bdb3-edf221902503)
## Installing Required Dependencies

To install the required dependencies, please run `pip install -r requirements.txt`

## MkDocs Plugins

The following plugins for MkDocs are being used:
- https://squidfunk.github.io/mkdocs-material/
- https://github.com/lukasgeiter/mkdocs-awesome-pages-plugin
