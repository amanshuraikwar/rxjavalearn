import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.observables.ConnectableObservable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.Timed;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Observable
                .just(1, 2, 3)
                .subscribe(val -> System.out.println(val+""));

        Observable
                .fromArray(new Integer[]{5, 6, 7})
                .subscribe(val -> System.out.println(val+"--"));

        List<Integer> list = new ArrayList<>();
        list.add(8);
        list.add(9);
        list.add(10);

        Observable
                .fromIterable(list)
                .subscribe(val -> System.out.println(val+"-><"));

        Observable
                .range(11, 5)
                .subscribe(val -> {
                   System.out.println(val+"---<<<>>>");
                });

        Observable
                .fromCallable(() -> 18)
                .subscribe(val -> System.out.println(val+",,>>"));

        // defer -- similar to fromCallable but returns an Observable
        // used because creating observable is itself a blocking task
        Observable
                .defer(() -> Observable.fromCallable(() -> 19))
                .subscribe(val -> System.out.println(val+"(())(())"));

        String strings[] = new String[]{"a", "bcd", "edfgiwejdj", "ab", "55939hhd"};

        Observable
                .fromArray(strings)
                .map(string -> "STRING --> "+string)
                .subscribe(System.out::println);

        Observable
                .fromArray(strings)
                .map(string -> string+"-")
                .filter(string -> string.length() > 4)
                .subscribe(System.out::println);

        // flatmap -- sequencing observables but if there is delay the
        //            emissions get overlapped
        Observable<String> obs1 = Observable.just("string1");
        obs1
                .flatMap(string -> obs2(string))
                .subscribe(System.out::println);

        // concatmap -- sequencing observables, if there is delay the
        //              emissions don't get overlapped, it waits for previous
        //              observable to complete before next emission
        obs1
                .concatMap(string -> obs2(string))
                .subscribe(System.out::println);

        // skip -- can skip first n emitted items
        // also .skip(time, timeUnit) which skips any item/s emitted before the specified time window.
        Observable
                .fromArray(1, 2, 3, 4)
                .skip(2)
                .subscribe(item -> System.out.println(item+"<><><>skip<><><>"));

        // take -- takes first n items
        // also .take(time, timeUnit) only takes the item/s emitted in the specified time window and ignores the rest.
        Observable
                .fromArray(1, 2, 3, 4)
                .take(2)
                .subscribe(item -> System.out.println(item+"<><><>take<><><>"));

        // first (completes immediately) and last (completes after parent completes) -- self explanatory

        // merge -- merges two observables, if one finishes -> completes
        // results overlapped
        Observable
                .merge(
                        Observable.fromArray(1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 14),
                        Observable.fromArray("a", "b", "c", "d", "e"))
                .subscribe(item -> System.out.println(item+""));

        // concat -- merges two observables
        // second is subscribed only if the first one completes
        // results not overlapped

        // zip pair wise zipping -> strict sequencing
        // not of emitted values are equal to number of emits in the sequence with less emits
        // extra values in any observable skipped
        Observable
                .zip(
                        Observable.fromArray(1, 2, 3, 4, 6, 7),
                        Observable.fromArray("-one", "-two", "-three"),
                        (item1, item2) -> item1.toString()+item2
                ).subscribe(System.out::println);

        // toList --> converts observable<T> to observable<List<T>>
        // if onComplete is not called then it never emits

        Observable
                .fromArray(10, 20, 30, 40, 50, 60)
                .toList()
                .subscribe(items -> {
                    for (int item : items) {
                        System.out.print(item+"converted-to-list--");
                    }
                    System.out.println("");
                });

        // reduce -> accumulator to reduce
        // should not be used to add items to a mutable ds
        // for that collect should be used
        Observable
                .fromArray(1, 2, 3, 4, 5, 6, 7)
                .reduce((i1, i2) -> i1+i2)
                .subscribe(item -> System.out.println(item+"-added"));

        // collect - for that this should be used
        Observable
                .fromArray("yo", "-", "haha", "-", "lol")
                .collect(
                        StringBuilder::new,
                        ((stringBuilder, s) -> stringBuilder.append(s)))
                .subscribe(System.out::println);

        // utility operators
        // doOnEach() doOnNext() doOnComplete() doOnError()
        // --> for side effects in addition to subscribe

        // cache --> to cache the emitted value
        Observable<Timed<Integer>> obsCache = Observable.just(1).timestamp().cache();
        obsCache.subscribe(item -> System.out.println(item.time()));
        obsCache.subscribe(item -> System.out.println(item.time()));

        // ObservableTransformers to use in .compose() to reuse a set of utility function calls

        // multi threading --
        // not running
        // only first subscribeOn() will be taken into account -- rest will b e ignored
        // many observeOn() can be taken into account
        Observable
                .create(source -> {
                    System.out.println("in subscribe");
                    source.onNext(1);
                    source.onNext(2);
                    source.onNext(3);
                })
                .subscribeOn(Schedulers.computation())
                .doOnNext(value -> System.out.println("(a) : " + value))
                .observeOn(Schedulers.newThread())
                .doOnNext(value -> System.out.println("(b) : " + value))
                .observeOn(Schedulers.newThread())
                .doOnNext(value -> System.out.println("(c) : " + value))
                .observeOn(Schedulers.io())
                .subscribe(value -> print("(d) : " + value));

        // true concurrency using flatmap
        Observable
                .fromArray(1, 2, 3, 4, 5)
                .subscribeOn(Schedulers.computation())
                .flatMap(
                        integer ->
                                Observable
                                        .fromCallable(() -> integer+100)
                                        .subscribeOn(Schedulers.computation()))
                .observeOn(Schedulers.computation())
                .subscribe(integer -> System.out.println(integer+""));

        // sorted
        Observable
                .range(1, 100)
                .filter(number -> number%5 == 0)
                .sorted((num1, num2) -> num2 - num1)
                .subscribe(num -> System.out.println(num+"--"));

        // Observable.fromCallable() and .defer() for lazy operation

        // we can also return a Completable() instead of an Observable()
        // which only has onCompleted() or onError()

        // Single() --> has only onSubscribe() onSuccess(T value) and onError()
        // Completable() --> does not emit values
        //                   has onSubscribe() onComplete() onError()
        // Maybe() --> mixture or Single() and Completable()
        //             has onSubscribe() onSuccess(T value) onError() and onComplete()

        // al above can be converted to Observable by toObservable()

        // todo multi casting in rx --> not understood

        // subjects
        // PublishSubject --> emits only values emitted after subscription
        // AsyncSubject --> emits only last value after observable completes
        // BehaviorSubject --> emits last value and all coming values after subscription
        // ReplaySubject --> emits all the values before and coming values after subscription
        // .hide() to prevent using the subject as Observer but as Observable

        // Disposable and CompositeDisposable

        // Flowable in place of Observable to counter BackPressure
        // different strategies
        // • BackpressureStrategy.ERROR - produces a MissingBackpressureException in the case when downstream cannot keep up with item emissions
        // • BackpressureStrategy.BUFFER - buffers all items until downstream consumes it (default buffer size is 128)
        // • BackpressureStrategy.DROP - drops the most recent item if downstream cannot keep up
        // • BackpressureStrategy.LATEST - keeps only the latest item overwriting any previous value if downstream cannot keep up
        // • BackpressureStrategy.MISSING - no backpressure is added to the Flowable (this is equiva-lent to using an Observable)

        // for changing buffer limit --> use overloaded observeOn()
        // .observeOn(
        //      AndroidSchedulers.mainThread(),
        //      false, --> whether or not an .onError() notification should cut ahead of .onNext() notifications, in this case, specifying false indicates that it can.
        //      bufferSize)

        // todo study back pressure properly

        // error handling
        // Observable.onErrorReturnItem() --> when error is received it will forward the item provided to it instead of error
        // Observable.onErrorReturn() --> when error is received, provided function will be called
        // Observable.onErrorResumeNext() --> on error it will pass the control to provided observable

        // retry
        /*
        networkClient.sendMessage(message).retry(10).subscribe(() -> {

        }, throwable -> {

        });
        */
        // retry at most 10 times ^^

        // if we don't want to retry blindly, use overrided version of retry()
        /*
        networkClient.sendMessage(message).retry(10, error -> { return !(error instanceof AuthenticationError);
        }).subscribe(() -> {

        }, throwable -> {

        });
        */

        // todo retryWhen()

        /*
        int[] x = new int[] {1, 2, 3, 5, 6, 7};

        int i = 0, j = x.length-1;
        int c = (j-i)/2;
        int sv = 7;

        while (i != j) {

             if (x[c] == sv) {
                 System.out.println("found");
                 break;
             }

             if (sv > x[c]) {
                 i = c;
             } else {
                 j = c;
             }

             System.out.println(i+" "+j+" "+c);
             c = i + (j-i)/2;
        }
*/
Observable<Integer> observable = Observable.just(1).doOnSubscribe(disposable -> System.out.println("observable-subscribed"));
Completable completable = new Completable() {
    @Override
    protected void subscribeActual(CompletableObserver s) {
        System.out.println("completable-subscribed");
    }
};
completable
        .andThen(observable)
        .subscribe(integer -> System.out.println("emitted:"+integer));
    }

    static void print(String s) {
        System.out.println(Thread.currentThread().getName()+"--"+s);
    }

    static Observable<String> obs2(String s) {
        return Observable.just(s+"string2");
    }
}
