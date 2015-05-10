package regression_tests;

import org.uispec4j.*;
import org.uispec4j.interception.BasicHandler;
import org.uispec4j.interception.MainClassAdapter;
import org.uispec4j.interception.WindowInterceptor;
import rpg.Framework;

public class UgmtSketchTest extends UISpecTestCase {

    protected Table contactTable;
    protected Tree categoryTree;
    protected Button newCategoryButton;
    protected Button newContactButton;
    protected TextBox firstNameField;
    protected TextBox lastNameField;
    protected TextBox phoneField;
    protected TextBox mobileField;
    protected TextBox emailField;
    protected Button applyButton;
    private TabGroup tabGroup;
    private Panel sketch;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        setAdapter(new MainClassAdapter(Framework.class));

        Window window = getMainWindow();
        
        System.out.println("Before window.getDescription");
        System.out.println(window.getDescription());
        System.out.println("After window.getDescription");
        
//        this.tabGroup = window.getTabGroup();
//        
//        tabGroup.selectTab("sketch");
//        this.sketch = tabGroup.getSelectedTab();
//        
//        System.out.println("Before getDescription");
//        System.out.println(sketch.getDescription());
//        System.out.println("after getDescription");
        
        UISpec4J.setWindowInterceptionTimeLimit(100);
    }
    
    public void testGetSketchTab() {
        System.out.println("Before getDescription");
//        System.out.println(sketch.getDescription());
        System.out.println("after getDescription");
        

    }
    
    public void testFindMainWindow() {
        
    }

//    protected void changeFields(String firstName, String lastName, String email, String phone, String mobile) {
//        firstNameField.setText(firstName);
//        lastNameField.setText(lastName);
//        emailField.setText(email);
//        phoneField.setText(phone);
//        mobileField.setText(mobile);
//    }
//
//    protected void createContact(String firstName, String lastName) {
//        createContact(firstName, lastName, "", "", "");
//    }
//
//    protected void createContact(String firstName, String lastName, String email, String phone, String mobile) {
//        newContactButton.click();
//        changeFields(firstName, lastName, email, phone, mobile);
//    }
//
//    protected void createCategory(String parentCategoryPath, String categoryName) {
//        Trigger trigger = newCategoryButton.triggerClick();
//        createCategory(parentCategoryPath, categoryName, trigger);
//    }
//
//    protected void createCategory(String parentCategoryPath, String categoryName, Trigger trigger) {
//        categoryTree.select(parentCategoryPath);
//        WindowInterceptor
//                .init(trigger)
//                .process(BasicHandler.init()
//                        .assertContainsText("Category name:")
//                        .setText(categoryName)
//                        .triggerButtonClick("OK"))
//                .run();
//    }
}
