import SwiftUI

struct ConfirmationDialogData: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let confirmTitle: String
    let cancelTitle: String
    let isDestructive: Bool
    let onConfirm: () -> Void

    init(
        title: String,
        message: String,
        confirmTitle: String = "Confirm",
        cancelTitle: String = "Cancel",
        isDestructive: Bool = false,
        onConfirm: @escaping () -> Void
    ) {
        self.title = title
        self.message = message
        self.confirmTitle = confirmTitle
        self.cancelTitle = cancelTitle
        self.isDestructive = isDestructive
        self.onConfirm = onConfirm
    }
}

struct SuccessDialogData: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let primaryAction: (title: String, action: () -> Void)
    let secondaryAction: (title: String, action: () -> Void)?
}

struct DialogModifier: ViewModifier {
    @Binding var confirmDialog: ConfirmationDialogData?
    @Binding var successDialog: SuccessDialogData?

    func body(content: Content) -> some View {
        content
            .alert(
                confirmDialog?.title ?? "",
                isPresented: Binding(
                    get: { confirmDialog != nil },
                    set: { if !$0 { confirmDialog = nil } }
                )
            ) {
                if let dialog = confirmDialog {
                    Button(dialog.cancelTitle, role: .cancel) {
                        confirmDialog = nil
                    }
                    Button(dialog.confirmTitle, role: dialog.isDestructive ? .destructive : nil) {
                        dialog.onConfirm()
                        confirmDialog = nil
                    }
                }
            } message: {
                if let dialog = confirmDialog {
                    Text(dialog.message)
                }
            }
            .alert(
                successDialog?.title ?? "",
                isPresented: Binding(
                    get: { successDialog != nil },
                    set: { if !$0 { successDialog = nil } }
                )
            ) {
                if let dialog = successDialog {
                    Button(dialog.primaryAction.title) {
                        dialog.primaryAction.action()
                        successDialog = nil
                    }
                    if let secondary = dialog.secondaryAction {
                        Button(secondary.title, role: .cancel) {
                            secondary.action()
                            successDialog = nil
                        }
                    }
                }
            } message: {
                if let dialog = successDialog {
                    Text(dialog.message)
                }
            }
    }
}

extension View {
    func dialogs(
        confirm: Binding<ConfirmationDialogData?>,
        success: Binding<SuccessDialogData?>
    ) -> some View {
        modifier(DialogModifier(confirmDialog: confirm, successDialog: success))
    }
}
