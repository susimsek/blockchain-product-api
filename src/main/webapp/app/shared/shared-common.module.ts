import {NgModule} from '@angular/core';

import {AppSharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent} from './';

@NgModule({
    imports: [AppSharedLibsModule],
    declarations: [JhiAlertComponent, JhiAlertErrorComponent],
    exports: [AppSharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent]
})
export class AppSharedCommonModule {}
